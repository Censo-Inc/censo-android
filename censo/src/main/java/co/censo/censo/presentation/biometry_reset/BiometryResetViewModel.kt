package co.censo.censo.presentation.biometry_reset

import Base64EncodedData
import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.censo.presentation.Screen
import co.censo.censo.util.isAuthResetApprovedFor
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.Authentication
import co.censo.shared.data.model.AuthenticationReset
import co.censo.shared.data.model.AuthenticationResetApproval
import co.censo.shared.data.model.AuthenticationResetStatus
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.CountDownTimerImpl
import co.censo.shared.util.VaultCountDownTimer
import co.censo.shared.util.asResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

@HiltViewModel
class BiometryResetViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val pollingVerificationTimer: VaultCountDownTimer,
    private val totpCodeTimer: VaultCountDownTimer,
    private val totpGenerator: TotpGenerator,
) : ViewModel() {

    var state by mutableStateOf(BiometryResetState())
        private set

    fun onStart() {
        updateOwnerState(ownerRepository.getOwnerStateValue())
    }

    fun onResume() {
        // setup polling timer to reload biometry reset approvals state
        pollingVerificationTimer.start(
            interval = CountDownTimerImpl.Companion.POLLING_VERIFICATION_COUNTDOWN,
        ) {
            if (state.userResponse !is Resource.Loading) {
                retrieveOwnerState(silent = true)
            }
        }

        totpCodeTimer.start(CountDownTimerImpl.Companion.UPDATE_COUNTDOWN) {
            nextTotpTimerTick()
        }
    }

    fun onPause() {
        pollingVerificationTimer.stopWithDelay(CountDownTimerImpl.Companion.VERIFICATION_STOP_DELAY)
        totpCodeTimer.stop()
    }

    fun onNavigate() {
        pollingVerificationTimer.stop()
    }

    //region Retrieve user and determine UI state
    fun retrieveOwnerState(silent: Boolean = false) {
        if (!silent) {
            state = state.copy(userResponse = Resource.Loading)
        }

        viewModelScope.launch {
            val ownerStateResource = ownerRepository.retrieveUser().map { it.ownerState }

            ownerStateResource.onSuccess { ownerState ->
                updateOwnerState(ownerState)
                ownerRepository.updateOwnerState(ownerState)
            }

            state = state.copy(userResponse = ownerStateResource)
        }
    }

    private fun updateOwnerState(ownerState: OwnerState) {
        if (ownerState !is OwnerState.Ready) {
            // other owner states are not supported on this view
            // navigate back to start of the app so it can fix itself
            state = state.copy(
                navigationResource = Screen.EntranceRoute
                    .navToAndPopCurrentDestination()
                    .asResource()
            )
            return
        }

        // update UI state
        when (val authReset = ownerState.authenticationReset) {
            null -> {
                if (ownerState.canRequestAuthenticationReset) {
                    receiveAction(BiometryResetAction.InitiateBiometryReset)
                } else if (state.replaceBiometryResource is Resource.Success) {
                    state = state.copy(uiState = UIState.Completed)
                }
            }

            is AuthenticationReset.AnotherDevice -> {
                state = state.copy(
                    authResetInitiated = true,
                    uiState = UIState.AnotherDevice
                )
            }

            is AuthenticationReset.ThisDevice -> {
                state = state.copy(
                    authResetInitiated = true,
                    approvers = ownerState.policy.approvers,
                    approvals = authReset.approvals
                )

                when {
                    authReset.status == AuthenticationResetStatus.Approved &&
                            state.uiState.notInList(UIState.EnrollNewBiometry, UIState.Facetec, UIState.Completed)
                    -> {
                        receiveAction(BiometryResetAction.AuthResetApproved)
                    }

                    state.uiState == UIState.ApproveBiometryReset &&
                            state.approvals.isAuthResetApprovedFor(state.selectedApprover)
                    -> {
                        state = state.copy(
                            selectedApprover = null,
                            uiState = UIState.SelectApprover
                        )
                    }

                    state.uiState == UIState.Initial -> {
                        state = state.copy(uiState = UIState.SelectApprover)
                    }
                }
            }
        }
    }
    //endregion

    //region TOTP generation
    private fun nextTotpTimerTick() {
        if (state.approvals.isEmpty()) return

        val now = Clock.System.now()
        val updatedCounter = now.epochSeconds.div(TotpGenerator.Companion.CODE_EXPIRATION)
        val secondsLeft = now.epochSeconds - (updatedCounter.times(TotpGenerator.CODE_EXPIRATION))

        state = if (state.counter != updatedCounter) {
            state.copy(
                secondsLeft = secondsLeft.toInt(),
                counter = updatedCounter,
                approverCodes = generateTotpCodes(state.approvals)
            )
        } else {
            state.copy(
                secondsLeft = secondsLeft.toInt(),
            )
        }
    }

    private fun generateTotpCodes(approvals: List<AuthenticationResetApproval>): Map<ParticipantId, String> {
        return approvals.associate { approval ->
            val code = totpGenerator.generateCode(
                secret = approval.totpSecret.value,
                counter = Clock.System.now().epochSeconds.div(TotpGenerator.CODE_EXPIRATION)
            )
            approval.participantId to code
        }
    }
    //endregion

    //region VM Actions
    fun receiveAction(action: BiometryResetAction) {
        when (action) {
            BiometryResetAction.InitiateBiometryReset -> onInitiateBiometryReset()
            BiometryResetAction.CancelBiometryReset -> onCancelBiometryReset()
            BiometryResetAction.CancelBiometryResetCancelled -> hideCloseConfirmationDialog()

            is BiometryResetAction.ApproverSelectionChanged -> onApproverSelected(action.approver)
            BiometryResetAction.ContinueWithSelectedApprover -> onContinueToApproval()

            BiometryResetAction.AuthResetApproved -> onAuthResetApproved()
            BiometryResetAction.EnrollNewBiometry -> onEnrollNewBiometry()

            BiometryResetAction.Completed -> onCompleted()

            BiometryResetAction.FacetecCancelled -> onBackClicked()
            BiometryResetAction.BackClicked -> onBackClicked()

            BiometryResetAction.Retry -> onRetry()
        }
    }

    private fun onEnrollNewBiometry() {
        state = state.copy(uiState = UIState.Facetec)
    }

    private fun onAuthResetApproved() {
        pollingVerificationTimer.stop()
        totpCodeTimer.stop()

        state = state.copy(uiState = UIState.EnrollNewBiometry)
    }

    private fun onRetry() {
        when {
            state.userResponse is Resource.Error -> retrieveOwnerState()
            state.initiateBiometryResetResource is Resource.Error -> receiveAction(BiometryResetAction.InitiateBiometryReset)
            state.cancelBiometryResetResource is Resource.Error -> receiveAction(BiometryResetAction.CancelBiometryReset)
            state.replaceBiometryResource is Resource.Error -> receiveAction(BiometryResetAction.AuthResetApproved)
        }
    }

    private fun onCompleted() {
        state = state.copy(uiState = UIState.Completed)
    }

    private fun onInitiateBiometryReset() {
        if (!state.authResetInitiated) {
            state = state.copy(
                authResetInitiated = true,
                initiateBiometryResetResource = Resource.Loading
            )

            viewModelScope.launch {
                val response = ownerRepository.requestAuthenticationReset()

                if (response is Resource.Success) {
                    updateOwnerState(response.data.ownerState)
                }

                state = state.copy(initiateBiometryResetResource = response)
            }
        }
    }

    private fun onCancelBiometryReset() {
        state = state.copy(
            showCancelConfirmationDialog = false,
            cancelBiometryResetResource = Resource.Loading
        )

        viewModelScope.launch {
            val response = ownerRepository.cancelAuthenticationReset()

            if (response is Resource.Success) {
                navigateToEntrance()
            }

            state = state.copy(cancelBiometryResetResource = response)
        }
    }

    private fun onApproverSelected(selectedApprover: Approver.TrustedApprover) {
        state = if (state.selectedApprover == selectedApprover) {
            state.copy(selectedApprover = null)
        } else {
            state.copy(selectedApprover = selectedApprover)
        }
    }

    private fun onContinueToApproval() {
        state = state.copy(uiState = UIState.ApproveBiometryReset)
    }

    private fun onBackClicked() {
        when (state.uiState) {
            UIState.SelectApprover -> {
                state = state.copy(showCancelConfirmationDialog = true)
            }

            UIState.ApproveBiometryReset -> {
                state = state.copy(uiState = UIState.SelectApprover)
            }

            UIState.EnrollNewBiometry -> {
                state = state.copy(showCancelConfirmationDialog = true)
            }

            UIState.Facetec -> {
                state = state.copy(uiState = UIState.EnrollNewBiometry)
            }

            else -> {}
        }
    }
    //endregion

    //region Facetec
    suspend fun onFaceScanReady(verificationId: BiometryVerificationId, biometry: FacetecBiometry): Resource<BiometryScanResultBlob> {
        state = state.copy(replaceBiometryResource = Resource.Loading)

        return viewModelScope.async {
            val replaceBiometryResponse = ownerRepository.replaceAuthentication(
                authentication = Authentication.FacetecBiometry(
                    faceScan = Base64EncodedData(biometry.faceScan),
                    auditTrailImage = Base64EncodedData(biometry.auditTrailImage),
                    lowQualityAuditTrailImage = Base64EncodedData(biometry.lowQualityAuditTrailImage),
                    verificationId = verificationId
                )
            )

            state = state.copy(
                replaceBiometryResource = replaceBiometryResponse,
            )

            if (replaceBiometryResponse is Resource.Success) {
                updateOwnerState(replaceBiometryResponse.data.ownerState)
                ownerRepository.updateOwnerState(replaceBiometryResponse.data.ownerState)
            }

            replaceBiometryResponse.map { it.scanResultBlob!! }
        }.await()
    }
    //endregion

    //region Reset and exit
    fun navigateToEntrance() {
        state = state.copy(navigationResource = Screen.EntranceRoute.navToAndPopCurrentDestination().asResource())
    }

    fun delayedResetNavigationResource() {
        viewModelScope.launch {
            delay(1000)
            state = state.copy(navigationResource = Resource.Uninitialized)
        }
    }

    private fun hideCloseConfirmationDialog() {
        state = state.copy(showCancelConfirmationDialog = false)
    }

    fun resetUserResponse() {
        state = state.copy(userResponse = Resource.Uninitialized)
    }

    fun resetInitiateBiometryResetResource() {
        state = state.copy(initiateBiometryResetResource = Resource.Uninitialized)
    }

    fun resetCancelBiometryResetResource() {
        state = state.copy(cancelBiometryResetResource = Resource.Uninitialized)
    }

    fun resetReplaceBiometryResource() {
        state = state.copy(replaceBiometryResource = Resource.Uninitialized)
    }
    //endregion
}