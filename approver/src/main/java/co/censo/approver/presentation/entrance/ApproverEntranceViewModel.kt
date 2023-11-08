package co.censo.approver.presentation.entrance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.approver.data.ApproverEntranceUIState
import co.censo.approver.presentation.Screen
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GoogleAuthError
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
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ApproverEntranceViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val guardianRepository: GuardianRepository,
    private val keyRepository: KeyRepository,
    private val pushRepository: PushRepository,
    private val authUtil: AuthUtil,
    private val secureStorage: SecurePreferences
) : ViewModel() {

    var state by mutableStateOf(ApproverEntranceState())
        private set

    fun getGoogleSignInClient() = authUtil.getGoogleSignInClient()

    fun onStart(invitationId: String?, recoveryParticipantId: String?) {
        if (invitationId != null) {
            guardianRepository.saveInvitationId(invitationId)
        }
        if (recoveryParticipantId != null) {
            guardianRepository.saveParticipantId(recoveryParticipantId)
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
                } else {
                    attemptRefresh(jwtToken)
                }
            } else {
                determineLoggedOutRoute()
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
            determineLoggedOutRoute()
        }
    }

    fun userHasSeenPushDialog() = pushRepository.userHasSeenPushDialog()

    fun setUserSeenPushDialog(seenDialog: Boolean) =
        pushRepository.setUserSeenPushDialog(seenDialog)

    private fun checkUserHasRespondedToNotificationOptIn() {
        viewModelScope.launch {
            if (pushRepository.userHasSeenPushDialog()) {
                submitNotificationTokenForRegistration()
                determineLoggedInRoute()
            } else {
                state = state.copy(showPushNotificationsDialog = true)
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
        viewModelScope.launch {
            try {
                val account = completedTask.await()

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

            } catch (e: Exception) {
                googleAuthFailure(GoogleAuthError.ErrorParsingIntent(e))
            }
        }
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

    private fun determineLoggedInRoute() {
        viewModelScope.launch {
            val participantId = guardianRepository.retrieveParticipantId()
            val invitationId = guardianRepository.retrieveInvitationId()

            when {
                participantId.isEmpty() && invitationId.isEmpty() -> {
                    val isActiveApprover = ownerRepository.retrieveUser().data
                        ?.let { it.guardianStates.any { it.invitationId != null } }
                        ?: false

                    state = state.copy(
                        uiState = ApproverEntranceUIState.LoggedInPasteLink(isActiveApprover)
                    )
                }
                participantId.isNotEmpty() -> triggerNavigation(RoutingDestination.ACCESS)
                invitationId.isNotEmpty() -> triggerNavigation(RoutingDestination.ONBOARDING)
            }
        }
    }

    private fun determineLoggedOutRoute() {
        viewModelScope.launch {
            val participantId = guardianRepository.retrieveParticipantId()
            val invitationId = guardianRepository.retrieveInvitationId()

            when {
                participantId.isNotEmpty() || invitationId.isNotEmpty() -> signInUI()
                else -> loggedOutPasteLinkUI()
            }
        }
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

    fun resetNavigationResource() {
        state = state.copy(navigationResource = Resource.Uninitialized)
    }

    fun resetSignInUserResource() {
        state = state.copy(signInUserResource = Resource.Uninitialized)
    }

    private fun resetForceUserToGrantCloudStorageAccess() {
        state = state.copy(forceUserToGrantCloudStorageAccess = ForceUserToGrantCloudStorageAccess())
    }

    fun finishPushNotificationDialog() {
        submitNotificationTokenForRegistration()
        state = state.copy(showPushNotificationsDialog = false)

        determineLoggedInRoute()
    }

    fun handleLoggedOutLink(clipboardContent: String?) {
        handleLink(clipboardContent) {
            determineLoggedOutRoute()
        }
    }

    fun handleLoggedInLink(clipboardContent: String?) {
        handleLink(clipboardContent) {
            determineLoggedInRoute()
        }
    }

    private fun handleLink(clipboardContent: String?, routing: () -> Unit) {
        if (clipboardContent == null) {
            state = state.copy(linkError = true)
            return
        }

        viewModelScope.launch {
            try {
                val censoLink = parseLink(clipboardContent)
                when (censoLink.host) {
                    "invite" -> {
                        guardianRepository.clearParticipantId()
                        guardianRepository.saveInvitationId(censoLink.identifier)
                        routing()
                    }

                    "access" -> {
                        guardianRepository.clearInvitationId()
                        guardianRepository.saveParticipantId(censoLink.identifier)
                        routing()
                    }

                    else -> state = state.copy(linkError = true)
                }
            } catch (e: Exception) {
                state = state.copy(linkError = true)
            }
        }
    }

    private fun loggedOutPasteLinkUI() {
        state = state.copy(uiState = ApproverEntranceUIState.LoggedOutPasteLink)
    }

    private fun signInUI() {
        state = state.copy(uiState = ApproverEntranceUIState.SignIn)
    }

    private fun triggerNavigation(routingDestination: RoutingDestination) {
        state = when (routingDestination) {
            RoutingDestination.ACCESS ->
                state.copy(navigationResource = Resource.Success(Screen.ApproverAccessScreen.route))

            RoutingDestination.ONBOARDING ->
                state.copy(navigationResource = Resource.Success(Screen.ApproverOnboardingScreen.route))
        }
    }

    data class CensoLink(
        val host: String,
        val identifier: String
    )
    private fun parseLink(link: String): CensoLink {
        val parts = link.split("//")
        if (parts.size != 2 || !parts[0].startsWith("censo")) {
            throw Exception("invalid link")
        }
        val routeAndIdentifier = parts[1].split("/")
        if (routeAndIdentifier.size != 2 && !setOf("access", "invite").contains(routeAndIdentifier[0])) {
            throw Exception("invalid link")
        }
        return CensoLink(routeAndIdentifier[0], routeAndIdentifier[1])
    }

    fun clearError() {
        state = state.copy(linkError = false)
    }
}