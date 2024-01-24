package co.censo.approver.presentation.entrance

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.approver.presentation.Screen
import co.censo.approver.BuildConfig
import co.censo.shared.CensoLink.Companion.ACCESS_TYPE
import co.censo.shared.CensoLink.Companion.INVITE_TYPE
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GoogleAuthError
import co.censo.shared.data.repository.ApproverRepository
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.data.storage.SecurePreferences
import co.censo.shared.parseLink
import co.censo.shared.util.AuthUtil
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.asResource
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
    private val approverRepository: ApproverRepository,
    private val keyRepository: KeyRepository,
    private val authUtil: AuthUtil,
    private val secureStorage: SecurePreferences
) : ViewModel() {

    var state by mutableStateOf(ApproverEntranceState())
        private set


    //region Lifecycle Methods
    fun onStart(invitationId: String?, accessParticipantId: String?, approvalId: String?, appLinkUri: Uri?) {
        if (invitationId != null) {
            approverRepository.saveInvitationId(invitationId)
            approverRepository.clearParticipantId()
            approverRepository.clearApprovalId()
        }
        if (accessParticipantId != null) {
            approverRepository.saveParticipantId(accessParticipantId)
            approverRepository.clearInvitationId()
        }

        if (approvalId != null) {
            approverRepository.saveApprovalId(approvalId)
            approverRepository.clearInvitationId()
        }

        state = state.copy(appLinkUri = appLinkUri)

        determineLoginState()
    }
    //endregion

    //region User Actions
    fun onLandingContinue() {
        if (state.loggedIn) {
            loggedInRouting()
        } else {
            signInUI()
        }
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

    fun navToSettingsScreen() {
        state = state.copy(
            navigationResource = Screen.ApproverSettingsScreen.navTo().asResource()
        )
    }

    fun navToResetLinks() {
        state = state.copy(
            navigationResource = Screen.ApproverResetLinksScreen.navTo().asResource()
        )
    }

    fun backToLanding() {
        determineLoginState()
    }
    //endregion

    //region Internal Methods
    fun getGoogleSignInClient() = authUtil.getGoogleSignInClient()

    private fun setLandingState(loggedIn: Boolean) {

        viewModelScope.launch {
            val isActiveApprover = if (loggedIn) {
                val retrieveUser = approverRepository.retrieveUser()

                if (retrieveUser is Resource.Success) {
                    retrieveUser.data.approverStates.any { approverState -> approverState.phase.isActiveApprover() }
                } else {
                    false
                }
            } else {
                false
            }
            state.appLinkUri?.let {
                state = state.copy(appLinkUri = null)
                if (it.scheme == "https" && (it.host == BuildConfig.LINK_HOST || it.host == BuildConfig.L1NK_HOST)) {
                    it.getQueryParameter("l")?.let { link ->
                        if (loggedIn) {
                            handleLoggedInLink(link)
                        } else {
                            handleLoggedOutLink(link)
                        }
                        true
                    }
                } else null
            } ?: run {
                val participantId = approverRepository.retrieveParticipantId()
                val invitationId = approverRepository.retrieveInvitationId()
                if (participantId.isEmpty() && invitationId.isEmpty()) {
                    state = state.copy(
                        uiState = ApproverEntranceUIState.Landing(isActiveApprover),
                        loggedIn = loggedIn
                    )
                } else {
                    state = state.copy(
                        loggedIn = loggedIn
                    )
                    onLandingContinue()
                }
            }
        }
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

            state = state.copy(signInUserResource = Resource.Loading)

            val signInUserResponse = ownerRepository.signInUser(
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
            val participantId = approverRepository.retrieveParticipantId()
            val invitationId = approverRepository.retrieveInvitationId()

            when {
                participantId.isEmpty() && invitationId.isEmpty() -> {
                    val retrieveUser = approverRepository.retrieveUser()

                    if (retrieveUser is Resource.Success) {
                        val isActiveApprover = retrieveUser.data.approverStates.any { approverState -> approverState.phase.isActiveApprover() }

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
            val participantId = approverRepository.retrieveParticipantId()
            val invitationId = approverRepository.retrieveInvitationId()

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

    fun googleAuthCancel() {
        state = state.copy(triggerGoogleSignIn = Resource.Uninitialized)
    }

    private fun handleLink(clipboardContent: String?, routing: () -> Unit) {
        if (clipboardContent == null) {
            state = state.copy(linkError = true)
            return
        }

        viewModelScope.launch {
            try {
                val censoLink = clipboardContent.parseLink()
                when (censoLink.type) {
                    INVITE_TYPE -> {
                        approverRepository.clearParticipantId()
                        approverRepository.saveInvitationId(censoLink.identifiers.mainId)
                        routing()
                    }

                    ACCESS_TYPE -> {
                        if (censoLink.identifiers.approvalId == null) {
                            approverRepository.clearInvitationId()
                            approverRepository.saveParticipantId(censoLink.identifiers.mainId)
                        } else {
                            approverRepository.clearInvitationId()
                            approverRepository.saveParticipantId(censoLink.identifiers.mainId)
                            approverRepository.saveApprovalId(censoLink.identifiers.approvalId!!)
                        }
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
                state.copy(
                    navigationResource = Screen.ApproverAccessScreen.navTo().copy(popUpToTop = true).asResource(),
                    uiState = ApproverEntranceUIState.Initial
                )

            RoutingDestination.ONBOARDING ->
                state.copy(
                    navigationResource = Screen.ApproverOnboardingScreen.navTo().copy(popUpToTop = true).asResource(),
                    uiState = ApproverEntranceUIState.Initial
                )
        }
    }
    //endregion

    //region Reset methods
    private fun resetForceUserToGrantCloudStorageAccess() {
        state = state.copy(forceUserToGrantCloudStorageAccess = ForceUserToGrantCloudStorageAccess())
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

    fun clearError() {
        state = state.copy(linkError = false)
    }
    //endregion
}