package co.censo.censo.presentation.login_id_reset

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.censo.presentation.Screen
import co.censo.shared.CensoLink.Companion.RESET_TYPE
import co.censo.shared.data.Resource
import co.censo.shared.data.model.AccessIntent
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.data.model.GoogleAuthError
import co.censo.shared.data.model.ResetToken
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.data.storage.SecurePreferences
import co.censo.shared.parseLink
import co.censo.shared.util.AuthUtil
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.sendError
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * LoginId Reset Flow
 *   Step 1: Collect reset links
 *   Step 2: Pre-login user with new Primary Auth (Google Sign In)
 *   Step 3: Biometry verification
 *   Step 4: Approver key recovery
 */

@HiltViewModel
class LoginIdResetViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository,
    private val authUtil: AuthUtil,
    private val secureStorage: SecurePreferences,
) : ViewModel() {

    var state by mutableStateOf(LoginIdResetState())
        private set

    fun getGoogleSignInClient() = authUtil.getGoogleSignInClient()

    fun receiveAction(action: LoginIdResetAction) {
        when (action) {
            is LoginIdResetAction.PasteLink -> onPasteLinkClicked(action.clipboardContent)
            is LoginIdResetAction.TokenReceived -> onTokenReceived(action.token)
            LoginIdResetAction.SelectGoogleId -> launchGoogleSignInFlow()
            LoginIdResetAction.Facescan -> launchFaceScan()
            LoginIdResetAction.KeyRecovery -> navigateToKeyRecovery()
            LoginIdResetAction.DetermineResetStep -> onDetermineResetStep()

            LoginIdResetAction.Exit -> onExit()

            LoginIdResetAction.Retry -> onRetry()
        }
    }

    fun onStart(token: String?) {
        viewModelScope.launch {
            if (secureStorage.retrieveJWT().isNotBlank()) {
                signOut()
            }

            if (!token.isNullOrBlank()) {
                receiveAction(LoginIdResetAction.TokenReceived(token))
            } else {
                receiveAction(LoginIdResetAction.DetermineResetStep)
            }
        }
    }

    private fun onPasteLinkClicked(clipboardContent: String?) {
        try {
            clipboardContent?.parseLink()?.let { parsedLink ->
                if (parsedLink.type == RESET_TYPE) {
                    receiveAction(LoginIdResetAction.TokenReceived(parsedLink.identifiers.mainId))
                } else {
                    state = state.copy(linkError = true)
                }
            }
        } catch (e: Exception) {
            state = state.copy(linkError = true)
        }
    }

    private fun onTokenReceived(token: String) {
        val storedTokens = secureStorage.retrieveResetTokens()

        if (!storedTokens.contains(token)) {
            val updatedTokens = storedTokens + token

            state = state.copy(collectedTokens = updatedTokens.size)
            secureStorage.saveResetTokens(updatedTokens)

        } else {
            state = state.copy(collectedTokens = storedTokens.size)
        }

        receiveAction(LoginIdResetAction.DetermineResetStep)
    }

    private fun onDetermineResetStep() {
        when {
            state.resetLoginIdResponse is Resource.Success -> {
                state = state.copy(resetStep = LoginIdResetStep.KeyRecovery)
            }
            state.idToken.isNotBlank() -> {
                state = state.copy(resetStep = LoginIdResetStep.Facetec)
            }
            state.requiredTokens <= state.collectedTokens -> {
                state = state.copy(resetStep = LoginIdResetStep.SelectLoginId)
            }
        }
    }

    private fun onRetry() {
        when {
            state.triggerGoogleSignIn is Resource.Error -> receiveAction(LoginIdResetAction.SelectGoogleId)
            state.createDeviceResponse is Resource.Error -> {
                resetCreateDeviceResponse()
                receiveAction(LoginIdResetAction.SelectGoogleId)
            }
            state.resetLoginIdResponse is Resource.Error -> {
                resetResetLoginIdResponse()
                receiveAction(LoginIdResetAction.Facescan)
            }
        }
    }

    private fun launchFaceScan() {
        state = state.copy(launchFacetec = true)
    }

    private fun navigateToKeyRecovery() {
        secureStorage.clearResetTokens()
        state = state.copy(navigationResource = Resource.Success(Screen.AccessApproval.withIntent(AccessIntent.RecoverOwnerKey)))
    }

    private fun onExit() {
        viewModelScope.launch {
            ownerRepository.signUserOut()
            secureStorage.clearResetTokens()
            state = state.copy(navigationResource = Resource.Success(Screen.EntranceRoute.route))
        }
    }

    private suspend fun signOut() {
        ownerRepository.signUserOut()
    }

    private fun launchGoogleSignInFlow() {
        state = state.copy(triggerGoogleSignIn = Resource.Success(Unit))
    }

    fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        viewModelScope.launch {
            try {
                val account = completedTask.await()
                preSignInUser(account.idToken)
            } catch (e: Exception) {
                googleAuthFailure(GoogleAuthError.ErrorParsingIntent(e))
            }
        }
    }

    private fun preSignInUser(jwt: String?) {
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

            val createDeviceResponse = ownerRepository.createDevice()
            state = state.copy(createDeviceResponse = createDeviceResponse)

            if (createDeviceResponse is Resource.Success) {
                state = state.copy(idToken = idToken)
                receiveAction(LoginIdResetAction.DetermineResetStep)
            }
        }
    }

    suspend fun onFaceScanReady(
        verificationId: BiometryVerificationId,
        biometry: FacetecBiometry
    ): Resource<BiometryScanResultBlob> {
        state = state.copy(resetLoginIdResponse = Resource.Loading())

        return viewModelScope.async {
            val response = ownerRepository.resetLoginId(
                idToken = state.idToken,
                resetTokens = secureStorage.retrieveResetTokens().map { ResetToken(it) },
                biometryVerificationId = verificationId,
                biometryData = biometry
            )

            state = state.copy(
                resetLoginIdResponse = response,
                launchFacetec = false
            )

            receiveAction(LoginIdResetAction.DetermineResetStep)

            response.map { it.scanResultBlob }
        }.await()
    }

    fun googleAuthFailure(googleAuthError: GoogleAuthError) {
        googleAuthError.exception.sendError(CrashReportingUtil.SignIn)
        state = state.copy(triggerGoogleSignIn = Resource.Error(exception = googleAuthError.exception))
    }

    fun googleAuthCancel() {
        state = state.copy(triggerGoogleSignIn = Resource.Uninitialized)
    }

    fun resetTriggerGoogleSignIn() {
        state = state.copy(triggerGoogleSignIn = Resource.Uninitialized)
    }

    fun resetNavigationResource() {
        state = state.copy(navigationResource = Resource.Uninitialized)
    }

    fun clearLinkError() {
        state = state.copy(linkError = false)
    }

    fun resetCreateDeviceResponse() {
        state = state.copy(createDeviceResponse = Resource.Uninitialized)
    }

    fun resetResetLoginIdResponse() {
        state = state.copy(resetLoginIdResponse = Resource.Uninitialized)
    }
}