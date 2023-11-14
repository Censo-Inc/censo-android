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
import co.censo.shared.data.repository.GuardianRepository
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.data.storage.SecurePreferences
import co.censo.shared.util.AuthUtil
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.projectLog
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

        determineLoginState()
    }

    fun onLandingContinue() {
        if (state.loggedIn) {
            loggedInRouting()
        } else {
            loggedOutRouting()
        }
    }

    private fun setLandingState(loggedIn: Boolean) {
        state = state.copy(uiState = ApproverEntranceUIState.Landing, loggedIn = loggedIn)
    }

    private fun determineLoginState() {
        val jwtToken = ownerRepository.retrieveJWT()
        if (jwtToken.isNotEmpty()) {
            val tokenValid = ownerRepository.checkJWTValid(jwtToken)

            if (tokenValid) {
                setLandingState(loggedIn = true)
            } else {
                attemptRefresh(jwtToken)
            }
        } else {
            setLandingState(loggedIn = false)
        }
    }

    private fun attemptRefresh(jwt: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val deviceKeyId = secureStorage.retrieveDeviceKeyId()
            authUtil.silentlyRefreshTokenIfInvalid(jwt, deviceKeyId, onDone = {
                val jwtToken = ownerRepository.retrieveJWT()
                if (jwtToken.isNotEmpty() && ownerRepository.checkJWTValid(jwtToken)) {
                    setLandingState(loggedIn = true)
                } else {
                    signOut()
                }
            })
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            ownerRepository.signUserOut()
            setLandingState(loggedIn = false)
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
                loggedInRouting()
                state.copy(
                    signInUserResource = signInUserResponse
                )
            } else {
                state.copy(signInUserResource = signInUserResponse)
            }
        }
    }

    private fun loggedInRouting() {
        viewModelScope.launch {
            val participantId = guardianRepository.retrieveParticipantId()
            val invitationId = guardianRepository.retrieveInvitationId()

            when {
                participantId.isEmpty() && invitationId.isEmpty() -> {
                    val retrieveUser = guardianRepository.retrieveUser()

                    if (retrieveUser is Resource.Success) {
                        val isActiveApprover = retrieveUser.data
                            ?.let { it.guardianStates.any { it.invitationId != null } }
                            ?: false

                        state = state.copy(
                            uiState = ApproverEntranceUIState.LoggedInPasteLink(isActiveApprover)
                        )
                    } else {
                        retrySignIn()
                    }
                }
                participantId.isNotEmpty() -> triggerNavigation(RoutingDestination.ACCESS)
                invitationId.isNotEmpty() -> triggerNavigation(RoutingDestination.ONBOARDING)
            }
        }
    }

    private fun loggedOutRouting() {
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
        googleAuthError.exception.sendError(CrashReportingUtil.SignIn)
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

    fun handleLoggedOutLink(clipboardContent: String?) {
        handleLink(clipboardContent) {
            loggedOutRouting()
        }
    }

    fun handleLoggedInLink(clipboardContent: String?) {
        handleLink(clipboardContent) {
            loggedInRouting()
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
        val parts = link.replace(Regex("[\\r\\n]+"), "").trim().split("//")
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

    fun setShowDeleteUserWarning() {
        state = state.copy(showDeleteUserWarningDialog = true)
    }

    fun resetShowDeleteUserWarning() {
        state = state.copy(showDeleteUserWarningDialog = false)
    }

    fun setShowDeleteUserConfirmDialog() {
        resetShowDeleteUserWarning()
        state = state.copy(showDeleteUserConfirmDialog = true)
    }

    fun resetShowDeleteUserConfirmDialog() {
        state = state.copy(showDeleteUserConfirmDialog = false)
    }

    fun deleteUser() {
        state = state.copy(deleteUserResource = Resource.Loading())
        resetShowDeleteUserConfirmDialog()

        viewModelScope.launch {
            //When deleting the approver user, we don't need to pass in the participantId
            val deleteUserResource = ownerRepository.deleteUser(null, true)

            state = state.copy(
                deleteUserResource = deleteUserResource
            )

            if (deleteUserResource is Resource.Success) {
                state =
                    state.copy(navigationResource = Resource.Success(Screen.ApproverEntranceRoute.route))
            }
        }
    }

    fun resetDeleteUserResource() {
        state = state.copy(deleteUserResource = Resource.Uninitialized)
    }
}