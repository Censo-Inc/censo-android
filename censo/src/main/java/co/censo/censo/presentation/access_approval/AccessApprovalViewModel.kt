package co.censo.censo.presentation.access_approval

import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.censo.presentation.Screen
import co.censo.censo.presentation.Screen.PolicySetupRoute.navToAndPopCurrentDestination
import co.censo.censo.util.TestUtil
import co.censo.censo.util.isApprovedFor
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.model.Access
import co.censo.shared.data.model.AccessIntent
import co.censo.shared.data.model.AccessStatus
import co.censo.shared.data.model.AccessApprovalStatus
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.CountDownTimerImpl
import co.censo.shared.util.VaultCountDownTimer
import co.censo.shared.util.asResource
import co.censo.shared.util.isDigitsOnly
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccessApprovalViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val pollingVerificationTimer: VaultCountDownTimer,
    private val keyValidationTrigger: MutableSharedFlow<String>,
) : ViewModel() {

    var state by mutableStateOf(AccessApprovalState())
        private set

    fun onStart(accessIntent: AccessIntent) {
        state = state.copy(accessIntent = accessIntent)

        updateOwnerState(ownerRepository.getOwnerStateValue(), updateSharedState = false)
    }

    fun onResume() {
        // setup polling timer to reload approvals state
        pollingVerificationTimer.start(
            interval = CountDownTimerImpl.Companion.POLLING_VERIFICATION_COUNTDOWN,
            skipFirstTick = true
        ) {
            if (state.userResponse !is Resource.Loading) {
                retrieveOwnerState(silent = true)
            }
        }
    }

    fun onPause() {
        pollingVerificationTimer.stopWithDelay(CountDownTimerImpl.Companion.VERIFICATION_STOP_DELAY)
    }

    fun onNavigate() {
        pollingVerificationTimer.stop()
    }

    fun retrieveOwnerState(silent: Boolean = false) {
        if (!silent) {
            state = state.copy(userResponse = Resource.Loading)
        }

        viewModelScope.launch {
            val ownerStateResource = ownerRepository.retrieveUser().map { it.ownerState }

            ownerStateResource.onSuccess { updateOwnerState(it) }

            state = state.copy(userResponse = ownerStateResource)
        }
    }

    private fun updateOwnerState(ownerState: OwnerState, updateSharedState: Boolean = true) {
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

        // update global state
        if (updateSharedState) {
            ownerRepository.updateOwnerState(ownerState)
        }

        // restore state
        when (val access = ownerState.access) {
            null -> initiateAccess()

            is Access.AnotherDevice -> {
                state = state.copy(
                    accessApprovalUIState = AccessApprovalUIState.AnotherDevice
                )
            }

            is Access.ThisDevice -> {
                state = state.copy(
                    access = access,
                    approvals = access.approvals,
                    approvers = ownerState.policy.approvers
                )

                if (access.status == AccessStatus.Available) {
                    if (ownerState.policy.approvers.all { it.isOwner } ||
                        (access.intent == AccessIntent.AccessPhrases && ownerState.timelockSetting.currentTimelockInSeconds != null)) {
                        // owner is single approver or we were previously timelocked, skip to view seed phrases
                        navigateIntentAware()
                    } else {
                        state = state.copy(accessApprovalUIState = AccessApprovalUIState.Approved)
                    }
                    pollingVerificationTimer.stop()
                } else if (access.status == AccessStatus.Timelocked) {
                    state = if (ownerState.policy.approvers.all { it.isOwner }) {
                        state.copy(
                            navigationResource = Screen.OwnerVaultScreen
                                .navToAndPopCurrentDestination()
                                .asResource()
                        )
                    } else {
                        state.copy(
                            accessApprovalUIState = AccessApprovalUIState.Approved,
                            isTimelocked = true
                        )
                    }
                } else if (state.accessApprovalUIState == AccessApprovalUIState.ApproveAccess && state.approvals.isApprovedFor(state.selectedApprover))  {
                    state = state.copy(
                        selectedApprover = null,
                        verificationCode = "",
                        waitingForApproval = false,
                        accessApprovalUIState = AccessApprovalUIState.SelectApprover
                    )
                } else if (state.accessApprovalUIState == AccessApprovalUIState.Initial) {
                    // determine initial UI state
                    state = state.copy(accessApprovalUIState = AccessApprovalUIState.SelectApprover)
                } else {
                    checkForRejections(access)
                }
            }
        }
    }

    fun setNavigateBackToHome() {
        state = state.copy(
            isTimelocked = false,
            navigationResource = Screen.OwnerVaultScreen
                .navToAndPopCurrentDestination()
                .asResource()
        )
    }

    private fun checkForRejections(access: Access.ThisDevice) {
        access.approvals.find { it.participantId == state.selectedApprover?.participantId }
            ?.let {
                if (state.waitingForApproval && it.status == AccessApprovalStatus.Rejected) {
                    state = state.copy(
                        waitingForApproval = false,
                        verificationCode = ""
                    )
                }
            }
    }

    fun initiateAccess() {
        if (state.initiateAccessResource !is Resource.Loading && state.cancelAccessResource is Resource.Uninitialized) {
            state = state.copy(initiateAccessResource = Resource.Loading)

            viewModelScope.launch {
                val response = ownerRepository.initiateAccess(state.accessIntent)

                if (response is Resource.Success) {
                    updateOwnerState(response.data.ownerState)
                }

                state = state.copy(initiateAccessResource = response)
            }
        }
    }

    fun cancelAccess() {
        state = state.copy(
            showCancelConfirmationDialog = false,
            cancelAccessResource = Resource.Loading
        )

        viewModelScope.launch {
            val response = ownerRepository.cancelAccess()

            if (response is Resource.Success) {
                ownerRepository.updateOwnerState(response.data.ownerState)
                approverKeyValidation(response.data.ownerState)
                state = state.copy(
                    access = null,
                    navigationResource = Screen.OwnerVaultScreen.navToAndPopCurrentDestination().asResource(),
                    cancelAccessResource = response,
                )
            } else if (response is Resource.Error && response.errorCode == 404) {
                state = state.copy(
                    access = null,
                    navigationResource = Screen.OwnerVaultScreen.navToAndPopCurrentDestination().asResource(),
                    cancelAccessResource = Resource.Uninitialized,
                )
            } else {
                state = state.copy(cancelAccessResource = Resource.Uninitialized)
            }
        }
    }

    private suspend fun approverKeyValidation(ownerState: OwnerState) {
        if (state.accessIntent == AccessIntent.RecoverOwnerKey) {
            (ownerState as? OwnerState.Ready)?.let { readyState ->
                readyState.policy.approvers.firstOrNull { it.isOwner }?.participantId?.let {
                    keyValidationTrigger.emit(it.value)
                }
            }
        }
    }

    fun updateVerificationCode(code: String) {
        if (state.submitTotpVerificationResource is Resource.Error) {
            state = state.copy(
                submitTotpVerificationResource = Resource.Uninitialized,
            )
        }

        if (code.isDigitsOnly()) {
            state = state.copy(verificationCode = code)

            if (code.length == TotpGenerator.CODE_LENGTH) {
                submitVerificationCode(state.selectedApprover!!.participantId, code)
            }
        }
    }

    private fun submitVerificationCode(participantId: ParticipantId, verificationCode: String) {
        state = state.copy(
            submitTotpVerificationResource = Resource.Loading,
            waitingForApproval = true
        )

        viewModelScope.launch {
            val submitVerificationResource = ownerRepository.submitAccessTotpVerification(
                participantId = participantId,
                verificationCode = verificationCode
            )

            submitVerificationResource.onSuccess {
                updateOwnerState(it.ownerState)
            }

            state = state.copy(submitTotpVerificationResource = submitVerificationResource)
        }
    }

    fun delayedResetNavigationResource() {
        viewModelScope.launch {
            delay(1000)
            state = state.copy(navigationResource = Resource.Uninitialized)
        }
    }

    fun onApproverSelected(selectedApprover: Approver.TrustedApprover) {
        state = if (state.selectedApprover == selectedApprover) {
            state.copy(selectedApprover = null)
        } else {
            state.copy(selectedApprover = selectedApprover)
        }
    }

    fun onContinue() {
        state = state.copy(accessApprovalUIState = AccessApprovalUIState.ApproveAccess)
    }

    fun onBackClicked() {
        when (state.accessApprovalUIState) {
            AccessApprovalUIState.SelectApprover -> {
                state = state.copy(showCancelConfirmationDialog = true)
            }

            AccessApprovalUIState.ApproveAccess -> {
                state = state.copy(accessApprovalUIState = AccessApprovalUIState.SelectApprover)
            }

            AccessApprovalUIState.Approved -> {
                state = state.copy(accessApprovalUIState = AccessApprovalUIState.ApproveAccess)
            }

            else -> {}
        }
    }

    fun navigateIntentAware() {
        val destination = when (state.accessIntent) {
            AccessIntent.AccessPhrases -> Screen.AccessSeedPhrases.route
            AccessIntent.ReplacePolicy -> Screen.PolicySetupRoute.removeApproversRoute()
            AccessIntent.RecoverOwnerKey -> Screen.OwnerKeyRecoveryRoute.route
        }

        state = state.copy(
            navigationResource = destination.navToAndPopCurrentDestination().asResource()
        )
    }

    fun hideCloseConfirmationDialog() {
        state = state.copy(showCancelConfirmationDialog = false)
    }

    fun setUIState(uiState: AccessApprovalUIState) {
        if (System.getProperty(TestUtil.TEST_MODE) == TestUtil.TEST_MODE_TRUE) {
            state = state.copy(accessApprovalUIState = uiState)
        }
    }
}