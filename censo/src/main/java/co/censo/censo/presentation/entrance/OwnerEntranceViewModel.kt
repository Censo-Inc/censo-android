package co.censo.censo.presentation.entrance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.censo.presentation.Screen
import co.censo.censo.presentation.Screen.PolicySetupRoute.navToAndPopCurrentDestination
import co.censo.shared.data.Resource
import co.censo.shared.data.model.Access
import co.censo.shared.data.model.AccessIntent
import co.censo.shared.data.model.GoogleAuthError
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.touVersion
import co.censo.shared.data.repository.AuthState
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.data.storage.SecurePreferences
import co.censo.shared.presentation.cloud_storage.CloudAccessState
import co.censo.shared.util.AuthUtil
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.asResource
import co.censo.shared.util.observeCloudAccessStateForAccessGranted
import co.censo.shared.util.sendError
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
class OwnerEntranceViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository,
    private val authUtil: AuthUtil,
    private val secureStorage: SecurePreferences,
    private val keyValidationTrigger: MutableSharedFlow<String>,
) : ViewModel() {

    var state by mutableStateOf(OwnerEntranceState())
        private set

    fun getGoogleSignInClient() = authUtil.getGoogleSignInClient()

    fun onStart(beneficiaryInviteId: String? = null) {
        state = state.copy(
            beneficiaryInviteId = beneficiaryInviteId
        )

        initAcceptedTermsOfUseVersion()

        checkUserHasValidToken()
    }

    private fun initAcceptedTermsOfUseVersion() {
        val acceptedTermsOfUseVersion = secureStorage.acceptedTermsOfUseVersion()
        state = state.copy(acceptedTermsOfUseVersion = acceptedTermsOfUseVersion)
    }

    fun setAcceptedTermsOfUseVersion(version: String) {
        secureStorage.setAcceptedTermsOfUseVersion(version)
        state = state.copy(
            acceptedTermsOfUseVersion = version,
            showAcceptTermsOfUse = false,
            signInUserResource = Resource.Loading
        )
    }

    fun deleteUser() {
        state = state.copy(
            deleteUserResource = Resource.Loading,
            triggerDeleteUserDialog = Resource.Uninitialized
        )

        viewModelScope.launch(Dispatchers.IO) {
            val response = ownerRepository.deleteUser(null)

            state = state.copy(
                deleteUserResource = response
            )

            if (response is Resource.Success) {
                state = state.copy(
                    showAcceptTermsOfUse = false,
                    userFinishedSetup = false,
                )
            }
        }
    }

    fun showDeleteUserDialog() {
        state = state.copy(triggerDeleteUserDialog = Resource.Success(Unit))
    }

    fun onCancelResetUser() {
        state = state.copy(triggerDeleteUserDialog = Resource.Uninitialized)
    }

    fun resetDeleteUserResource() {
        state = state.copy(deleteUserResource = Resource.Uninitialized)
    }

    private fun checkUserHasValidToken() {
        viewModelScope.launch {
            val jwtToken = ownerRepository.retrieveJWT()
            if (jwtToken.isNotEmpty()) {
                val tokenValid = ownerRepository.checkJWTValid(jwtToken)

                if (tokenValid) {
                    userAuthenticated()
                } else {
                    attemptRefresh(jwtToken)
                }
            } else {
                resetSignInUserResource()
            }
        }
    }

    private fun attemptRefresh(jwt: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val deviceKeyId = secureStorage.retrieveDeviceKeyId()

            authUtil.silentlyRefreshTokenIfInvalid(
                jwt = jwt,
                deviceKeyId = deviceKeyId,
                onDone = {
                    checkUserTokenAfterRefreshAttempt()
                }
            )
        }
    }

    private fun checkUserTokenAfterRefreshAttempt() {
        try {
            val jwtToken = ownerRepository.retrieveJWT()
            if (jwtToken.isEmpty() || !ownerRepository.checkJWTValid(jwtToken)) {
                signUserOutAfterAttemptedTokenRefresh()
            } else {
                userAuthenticated()
            }
        } catch (e: Exception) {
            signUserOutAfterAttemptedTokenRefresh()
        }
    }

    private fun signUserOutAfterAttemptedTokenRefresh() {
        viewModelScope.launch {
            try {
                ownerRepository.signUserOut()
                resetSignInUserResource()
            } catch (e: Exception) {
                resetSignInUserResource()
            }
        }
    }

    private fun handleCloudStorageAccessGranted(jwt: String?) {
        signInUser(jwt)
    }

    fun startGoogleSignInFlow() {
        state = state.copy(triggerGoogleSignIn = Resource.Success(Unit))
    }

    fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        viewModelScope.launch {
            try {
                val account = completedTask.await()

                if (!account.grantedScopes.contains(Scope(DriveScopes.DRIVE_FILE))) {
                    keyRepository.updateCloudAccessState(CloudAccessState.AccessRequired)
                    observeCloudAccessStateForAccessGranted(
                        coroutineScope = this, keyRepository = keyRepository
                    ) {
                        handleCloudStorageAccessGranted(account.idToken)
                    }
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

            if (signInUserResponse is Resource.Success) {
                userAuthenticated()
                retrieveOwnerStateSync()
                state = state.copy(
                    signInUserResource = if (state.showAcceptTermsOfUse) signInUserResponse else Resource.Loading,
                    userIsOnboarding = state.preFetchedUserResponse.success()?.data?.ownerState?.onboarding() ?: false
                )
            } else {
                state = state.copy(signInUserResource = signInUserResponse)
            }
        }
    }

    private fun userAuthenticated() {
        state = state.copy(
            userFinishedSetup = true,
            showAcceptTermsOfUse = state.acceptedTermsOfUseVersion != touVersion
        )
        ownerRepository.updateAuthState(AuthState.LOGGED_IN)
    }

    fun googleAuthFailure(googleAuthError: GoogleAuthError) {
        googleAuthError.exception.sendError(CrashReportingUtil.SignIn)
        state = state.copy(triggerGoogleSignIn = Resource.Error(exception = googleAuthError.exception))
    }

    fun googleAuthCancel() {
        state = state.copy(triggerGoogleSignIn = Resource.Uninitialized)
    }

    fun retrySignIn() {
        startGoogleSignInFlow()
    }

    fun resetTriggerGoogleSignIn() {
        state = state.copy(triggerGoogleSignIn = Resource.Uninitialized)
    }

    fun resetUserFinishedSetup() {
        state = state.copy(userFinishedSetup = false)
    }

    fun resetSignInUserResource() {
        state = state.copy(signInUserResource = Resource.Uninitialized)
    }

    private suspend fun retrieveOwnerStateSync() {
        val preFetchedUserResponse = ownerRepository.retrieveUser()
        state = state.copy(preFetchedUserResponse = preFetchedUserResponse)
    }

    fun retrieveOwnerStateAndNavigate() {
        viewModelScope.launch {
            if (state.preFetchedUserResponse is Resource.Uninitialized) {
                state = state.copy(userResponse = Resource.Loading)
                retrieveOwnerStateSync()
            }

            val userResponse = state.preFetchedUserResponse

            state = if (userResponse is Resource.Success) {
                // update global state
                ownerRepository.updateOwnerState(userResponse.data.ownerState)

                val ownerState = userResponse.data.ownerState
                loggedInRouting(ownerState)
                approverKeyValidation(ownerState)

                state.copy(userResponse = userResponse)
            } else if (userResponse is Resource.Error && userResponse.errorCode == 404) {
                ownerRepository.clearJWT()
                state.copy(userResponse = Resource.Uninitialized, signInUserResource = Resource.Uninitialized)
            } else {
                state.copy(userResponse = userResponse, signInUserResource = Resource.Uninitialized)
            }
        }
    }

    private suspend fun approverKeyValidation(ownerState: OwnerState) {
        (ownerState as? OwnerState.Ready)?.let { readyState ->
            val hasExternalApprovers = readyState.policy.approvers.any { !it.isOwner }
            val ownerParticipantId = readyState.policy.approvers.firstOrNull { it.isOwner }?.participantId

            if (hasExternalApprovers && ownerParticipantId != null) {
                keyValidationTrigger.emit(ownerParticipantId.value)
            }
        }
    }

    private fun loggedInRouting(ownerState: OwnerState) {
        val destination =
            if (!state.beneficiaryInviteId.isNullOrEmpty() && ownerState !is OwnerState.Beneficiary) {
                Screen.AcceptBeneficiaryInvitation.buildNavRoute(state.beneficiaryInviteId!!)
            } else {
                when (ownerState) {
                    is OwnerState.Ready -> {
                        if (ownerState.authenticationReset != null) {
                            Screen.BiometryResetRoute.route
                        } else if (ownerState.onboarded) {
                            val access = ownerState.access
                            if (access != null && access is Access.ThisDevice && access.intent != AccessIntent.RecoverOwnerKey) {
                                Screen.AccessApproval.withIntent(intent = access.intent)
                            } else {
                                Screen.OwnerVaultScreen.route
                            }
                        } else {
                            Screen.EnterPhraseRoute.buildNavRoute(
                                masterPublicKey = ownerState.vault.publicMasterEncryptionKey,
                                welcomeFlow = true
                            )
                        }
                    }

                    is OwnerState.Initial -> Screen.InitialPlanSetupRoute.route

                    is OwnerState.Empty -> Screen.EntranceRoute.route // can never happen

                    is OwnerState.Beneficiary -> Screen.Beneficiary.route
                }
            }

        state = state.copy(
            navigationResource = destination.navToAndPopCurrentDestination().asResource()
        )
    }

    fun startLoginIdRecovery() {
        state = state.copy(navigationResource = Screen.LoginIdResetRoute.navTo().asResource())
    }

    fun resetNavigationResource() {
        state = state.copy(navigationResource = Resource.Uninitialized)
    }
}