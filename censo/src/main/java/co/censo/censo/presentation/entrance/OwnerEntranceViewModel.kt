package co.censo.censo.presentation.entrance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.censo.presentation.Screen
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GoogleAuthError
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.AccessIntent
import co.censo.shared.data.model.touVersion
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
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
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val ownerStateFlow: MutableStateFlow<Resource<OwnerState>>,
) : ViewModel() {

    var state by mutableStateOf(OwnerEntranceState())
        private set

    fun getGoogleSignInClient() = authUtil.getGoogleSignInClient()

    fun onStart() {
        initAcceptedTermsOfUseVersion()

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
                idToken = idToken
            )

            state = if (signInUserResponse is Resource.Success) {
                userAuthenticated()
                state.copy(
                    signInUserResource = signInUserResponse
                )
            } else {
                state.copy(signInUserResource = signInUserResponse)
            }
        }
    }

    private fun userAuthenticated() {
        state = state.copy(
            userFinishedSetup = true,
            showAcceptTermsOfUse = state.acceptedTermsOfUseVersion != touVersion
        )
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

    fun resetUserFinishedSetup() {
        state = state.copy(userFinishedSetup = false)
    }

    fun resetSignInUserResource() {
        state = state.copy(signInUserResource = Resource.Uninitialized)
    }

    private fun resetForceUserToGrantCloudStorageAccess() {
        state = state.copy(forceUserToGrantCloudStorageAccess = ForceUserToGrantCloudStorageAccess())
    }

    fun retrieveOwnerStateAndNavigate() {
        state = state.copy(userResponse = Resource.Loading())

        viewModelScope.launch {
            val userResponse = ownerRepository.retrieveUser()

            state = if (userResponse is Resource.Success) {
                // update global state
                ownerStateFlow.tryEmit(userResponse.map { it.ownerState })

                loggedInRouting(userResponse.data!!.ownerState)

                state.copy(userResponse = userResponse)
            } else if (userResponse is Resource.Error && userResponse.errorCode == 404) {
                ownerRepository.clearJWT()
                state.copy(userResponse = Resource.Uninitialized, signInUserResource = Resource.Uninitialized)
            } else {
                state.copy(userResponse = userResponse, signInUserResource = Resource.Uninitialized)
            }
        }
    }

    private fun loggedInRouting(ownerState: OwnerState) {
        state = if (ownerState is OwnerState.Ready && ownerState.vault.seedPhrases.isNotEmpty()) {
            val destination = when {
                ownerState.access != null -> Screen.AccessApproval.withIntent(intent = AccessIntent.AccessPhrases)
                else -> Screen.OwnerVaultScreen.route
            }
            state.copy(navigationResource = Resource.Success(destination))
        } else {
            state.copy(navigationResource = Resource.Success(Screen.OwnerWelcomeScreen.route))
        }
    }

    fun resetNavigationResource() {
        state = state.copy(navigationResource = Resource.Uninitialized)
    }
}