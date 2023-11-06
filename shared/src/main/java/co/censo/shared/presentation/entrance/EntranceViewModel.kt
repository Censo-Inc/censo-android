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
import co.censo.shared.data.storage.SecurePreferences
import co.censo.shared.util.AuthUtil
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.sendError
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.services.drive.DriveScopes
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
    private val pushRepository: PushRepository,
    private val authUtil: AuthUtil,
    private val secureStorage: SecurePreferences
) : ViewModel() {

    var state by mutableStateOf(EntranceState())
        private set

    fun getGoogleSignInClient() = authUtil.getGoogleSignInClient()

    fun onOwnerStart() {
        initAcceptedTermsOfUseVersion()

        state = state.copy(ownerApp = true)

        checkUserHasValidToken()
    }

    fun onGuardianStart(invitationId: String?, recoveryParticipantId: String?) {
        initAcceptedTermsOfUseVersion()

        if (invitationId != null) {
            guardianRepository.saveInvitationId(invitationId)
        }
        if (recoveryParticipantId != null) {
            guardianRepository.saveParticipantId(recoveryParticipantId)
        }

        state = state.copy(ownerApp = false)

        checkUserHasValidToken()
    }

    private fun initAcceptedTermsOfUseVersion() {
        state = state.copy(acceptedTermsOfUseVersion = secureStorage.acceptedTermsOfUseVersion())
    }

    fun setAcceptedTermsOfUseVersion(version: String) {
        secureStorage.setAcceptedTermsOfUseVersion(version)
        state = state.copy(acceptedTermsOfUseVersion = version, showAcceptTermsOfUse = false)
    }

    private fun checkUserHasValidToken() {
        viewModelScope.launch {
            val jwtToken = ownerRepository.retrieveJWT()
            if (jwtToken.isNotEmpty()) {
                val tokenValid = ownerRepository.checkJWTValid(jwtToken)

                if (tokenValid) {
                    checkUserHasRespondedToNotificationOptIn()
                } else {
                    attemptRefresh(jwtToken)
                }
            } else {
                state = state.copy(
                    signInUserResource = Resource.Uninitialized
                )
            }
        }
    }

    private fun attemptRefresh(jwt: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val deviceKeyId = secureStorage.retrieveDeviceKeyId()
            authUtil.silentlyRefreshTokenIfInvalid(jwt, deviceKeyId, onDone = {
                checkUserTokenAfterRefreshAttempt()
            })
        }
    }

    private fun checkUserTokenAfterRefreshAttempt() {
        val jwtToken = ownerRepository.retrieveJWT()
        if (jwtToken.isNotEmpty() && ownerRepository.checkJWTValid(jwtToken)) {
            checkUserHasRespondedToNotificationOptIn()
        } else {
            signUserOutAfterAttemptedTokenRefresh()
        }
    }

    private fun signUserOutAfterAttemptedTokenRefresh() {
        viewModelScope.launch {
            ownerRepository.signUserOut()
            resetSignInUserResource()
        }
    }

    fun userHasSeenPushDialog() = pushRepository.userHasSeenPushDialog()

    fun setUserSeenPushDialog(seenDialog: Boolean) =
        pushRepository.setUserSeenPushDialog(seenDialog)

    private fun checkUserHasRespondedToNotificationOptIn() {
        viewModelScope.launch {
            if (pushRepository.userHasSeenPushDialog()) {
                submitNotificationTokenForRegistration()

                if (state.ownerApp) triggerOwnerNavigation() else triggerApproverNavigation()
            } else {
                state = state.copy(
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
                e.sendError(CrashReportingUtil.SubmitNotificationToken)
            }
        }
    }

    fun handleCloudStorageAccessGranted() {
        signInUser(state.forceUserToGrantCloudStorageAccess.jwt)
        resetForceUserToGrantCloudStorageAccess()
    }

    fun startGoogleSignInFlow() {
        state = state.copy(triggerGoogleSignIn = Resource.Success(Unit))
    }

    fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        authUtil.getAccountFromSignInTask(
            completedTask,
            onSuccess = { account ->
                if (!account.grantedScopes.contains(Scope(DriveScopes.DRIVE_FILE))) {
                    state = state.copy(
                        forceUserToGrantCloudStorageAccess = ForceUserToGrantCloudStorageAccess(
                            requestAccess = true,
                            jwt = account.idToken
                        )
                    )
                } else {
                    signInUser(account.idToken)
                }
            },
            onException = { exception ->
                googleAuthFailure(
                    GoogleAuthError.ErrorParsingIntent(
                        exception
                    )
                )
            })
    }


    private fun signInUser(jwt: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (jwt.isNullOrEmpty()) {
                googleAuthFailure(GoogleAuthError.MissingCredentialId)
                return@launch
            }

            ownerRepository.saveJWT(jwt)

            val idToken = try {
                ownerRepository.verifyToken(jwt)
            } catch (e: Exception) {
                e.sendError(CrashReportingUtil.SignIn)
                googleAuthFailure(GoogleAuthError.FailedToVerifyId(e))
                return@launch
            }

            if (idToken == null) {
                Exception().sendError(CrashReportingUtil.SignIn)
                googleAuthFailure(GoogleAuthError.InvalidToken)
                return@launch
            }


            if (!keyRepository.hasKeyWithId(idToken)) {
                try {
                    keyRepository.createAndSaveKeyWithId(idToken)
                } catch (e: Exception) {
                    e.sendError(CrashReportingUtil.SignIn)
                    googleAuthFailure(GoogleAuthError.FailedToCreateKeyWithId(e))
                }
            } else {
                keyRepository.setSavedDeviceId(idToken)
            }

            state = state.copy(signInUserResource = Resource.Loading())

            val signInUserResponse = ownerRepository.signInUser(
                jwtToken = jwt,
                idToken = idToken
            )

            state = if (signInUserResponse is Resource.Success) {
                checkUserHasRespondedToNotificationOptIn()
                state.copy(
                    signInUserResource = signInUserResponse
                )
            } else {
                state.copy(signInUserResource = signInUserResponse)
            }
        }
    }

    private fun triggerOwnerNavigation() {
        state = state.copy(
            userFinishedSetup = Resource.Success(SharedScreen.OwnerRoutingScreen.route),
            showAcceptTermsOfUse = state.acceptedTermsOfUseVersion == ""
        )
    }

    private fun triggerApproverNavigation() {
        state = state.copy(
            userFinishedSetup = Resource.Success(SharedScreen.ApproverRoutingScreen.route)
        )
    }

    fun googleAuthFailure(googleAuthError: GoogleAuthError) {
        state = state.copy(triggerGoogleSignIn = Resource.Error(exception = googleAuthError.exception))
    }

    fun retrySignIn() {
        startGoogleSignInFlow()
    }

    fun resetTriggerGoogleSignIn() {
        state = state.copy(triggerGoogleSignIn = Resource.Uninitialized)
    }

    fun resetUserFinishedSetup() {
        state = state.copy(userFinishedSetup = Resource.Uninitialized)
    }

    fun resetSignInUserResource() {
        state = state.copy(signInUserResource = Resource.Uninitialized)
    }

    private fun resetForceUserToGrantCloudStorageAccess() {
        state = state.copy(forceUserToGrantCloudStorageAccess = ForceUserToGrantCloudStorageAccess())
    }

    fun finishPushNotificationDialog() {
        submitNotificationTokenForRegistration()
        state = state.copy(showPushNotificationsDialog = Resource.Uninitialized)

        if (state.ownerApp) triggerOwnerNavigation() else triggerApproverNavigation()
    }
}