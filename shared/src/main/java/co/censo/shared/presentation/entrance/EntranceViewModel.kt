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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 *
 * General Android Owner Flow
 * Step 1: Login user with Primary Auth (OneTap)
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

    fun onOwnerStart() {
        checkUserHasValidToken()
    }

    fun onGuardianStart(invitationId: String?, participantId: String?) {
        if (invitationId != null) {
            guardianRepository.saveInvitationId(invitationId)
        }
        if (participantId != null) {
            guardianRepository.saveParticipantId(participantId)
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

    fun startOneTapFlow() {
        state = state.copy(triggerOneTap = Resource.Success(Unit))
    }

    fun oneTapSuccess(googleIdCredential: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val idToken = try {
                ownerRepository.verifyToken(googleIdCredential)
            } catch (e: Exception) {
                oneTapFailure(OneTapError.FailedToVerifyId(e))
                return@launch
            }

            if (idToken == null) {
                oneTapFailure(OneTapError.InvalidToken)
                return@launch
            }

            if (!keyRepository.hasKeyWithId(idToken)) {
                keyRepository.createAndSaveKeyWithId(idToken)
            } else {
                keyRepository.setSavedDeviceId(idToken)
            }

            ownerRepository.saveJWT(googleIdCredential)

            val createUserResponse = ownerRepository.createUser(
                jwtToken = googleIdCredential,
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

    fun oneTapFailure(oneTapError: OneTapError) {
        state = state.copy(triggerOneTap = Resource.Error(exception = oneTapError.exception))
    }

    fun retryCreateUser() {
        startOneTapFlow()
    }

    fun resetTriggerOneTap() {
        state = state.copy(triggerOneTap = Resource.Uninitialized)
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
}