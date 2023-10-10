package co.censo.shared.presentation.entrance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.SharedScreen
import co.censo.shared.data.Resource
import co.censo.shared.data.networking.PushBody
import co.censo.shared.data.repository.GuardianRepository
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.data.repository.PushRepository
import co.censo.shared.data.repository.PushRepositoryImpl
import co.censo.shared.util.projectLog
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 *
 * General Android Owner Flow
 * Step 1: Login user with Primary Auth (Google Sign In)
 *       - If login error occurs notify user
 * Step 2: Handle Device Key Work: TODO
 * Step 3: Send user to Home screen
 *
 */

@HiltViewModel
class EntranceViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val guardianRepository: GuardianRepository,
    private val keyRepository: KeyRepository,
    private val pushRepository: PushRepository
) : ViewModel() {

    var state by mutableStateOf(EntranceState())
        private set

    fun onStart(invitationId: String, guardianEntrance: Boolean) {

        if (guardianEntrance && invitationId.isNotEmpty()) {
            guardianRepository.saveInvitationId(invitationId)
        }

        checkUserHasValidToken()
    }

    private fun checkUserHasValidToken() {
        viewModelScope.launch {
            val jwtToken = ownerRepository.retrieveJWT()
            if (jwtToken.isNotEmpty()) {
                val tokenValid = ownerRepository.checkJWTValid(jwtToken)

                if (tokenValid) {
                    checkUserHasRespondedToNotificationOptIn()
                    state = state.copy(
                        userFinishedSetup = Resource.Success(SharedScreen.HomeRoute.route)
                    )
                } else {
                    //TODO: Sign out the user here
                }
            }
        }
    }

    fun userHasSeenPushDialog() = pushRepository.userHasSeenPushDialog()

    fun setUserSeenPushDialog(seenDialog: Boolean) =
        pushRepository.setUserSeenPushDialog(seenDialog)

    private fun checkUserHasRespondedToNotificationOptIn() {
        viewModelScope.launch {
            state = if (pushRepository.userHasSeenPushDialog()) {
                submitNotificationTokenForRegistration()
                state.copy(
                    userFinishedSetup = Resource.Success(SharedScreen.HomeRoute.route)
                )
            } else {
                state.copy(
                    showPushNotificationsDialog = Resource.Success(Unit)
                )
            }
        }
    }

    private fun submitNotificationTokenForRegistration() {
        viewModelScope.launch {
            try {
                val token = pushRepository.retrievePushToken()
                if (token.isNotEmpty()) {
                    val pushBody = PushBody(
                        deviceType = PushRepositoryImpl.DEVICE_TYPE,
                        token = token
                    )
                    pushRepository.addPushNotification(pushBody = pushBody)
                }
            } catch (e: Exception) {
                projectLog(message = "Exception caught while trying to submit notif token")
            }
        }
    }

    fun startGoogleSignInFlow() {
        state = state.copy(triggerGoogleSignIn = Resource.Success(Unit))
    }

    fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            createUser(account.idToken)
        } catch (e: ApiException) {
            googleAuthFailure(GoogleAuthError.ErrorParsingIntent(e))
        }
    }


    fun createUser(jwt: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (jwt.isNullOrEmpty()) {
                googleAuthFailure(GoogleAuthError.MissingCredentialId)
                return@launch
            }

            ownerRepository.saveJWT(jwt)

            val idToken = try {
                ownerRepository.verifyToken(jwt)
            } catch (e: Exception) {
                googleAuthFailure(GoogleAuthError.FailedToVerifyId(e))
                return@launch
            }

            if (idToken == null) {
                googleAuthFailure(GoogleAuthError.InvalidToken)
                return@launch
            }

            if (!keyRepository.hasKeyWithId(idToken)) {
                keyRepository.createAndSaveKeyWithId(idToken)
            } else {
                keyRepository.setSavedDeviceId(idToken)
            }

            val createUserResponse = ownerRepository.createUser(
                jwtToken = jwt,
                idToken = idToken
            )

            state = if (createUserResponse is Resource.Success) {
                checkUserHasRespondedToNotificationOptIn()
                state.copy(
                    createUserResource = createUserResponse
                )
            } else {
                state.copy(createUserResource = createUserResponse)
            }
        }
    }

    fun googleAuthFailure(googleAuthError: GoogleAuthError) {
        state = state.copy(triggerGoogleSignIn = Resource.Error(exception = googleAuthError.exception))
    }

    fun retryCreateUser() {
        startGoogleSignInFlow()
    }

    fun resetTriggerGoogleSignIn() {
        state = state.copy(triggerGoogleSignIn = Resource.Uninitialized)
    }

    fun resetUserFinishedSetup() {
        state = state.copy(userFinishedSetup = Resource.Uninitialized)
    }

    fun resetCreateOwnerResource() {
        state = state.copy(createUserResource = Resource.Uninitialized)
    }

    fun finishPushNotificationDialog() {
        submitNotificationTokenForRegistration()
        state = state.copy(
            userFinishedSetup = Resource.Success(SharedScreen.HomeRoute.route),
            showPushNotificationsDialog = Resource.Uninitialized
        )
    }

    fun signUserOut() {
        viewModelScope.launch {
            ownerRepository.signUserOut()
        }
    }
}