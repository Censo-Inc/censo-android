package co.censo.censo.presentation.access_approval

import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.model.ApprovalStatus
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.Recovery
import co.censo.shared.data.model.RecoveryIntent
import co.censo.shared.data.model.RecoveryStatus
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.CountDownTimerImpl
import co.censo.shared.util.VaultCountDownTimer
import co.censo.censo.presentation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccessApprovalViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val ownerStateFlow: MutableStateFlow<Resource<OwnerState>>,
    private val pollingVerificationTimer: VaultCountDownTimer
) : ViewModel() {

    var state by mutableStateOf(AccessApprovalState())
        private set

    fun onStart() {
        viewModelScope.launch {
            val ownerState = ownerStateFlow.value
            if (ownerState is Resource.Success) {
                updateOwnerState(ownerState.data!!)
            }

            // setup polling timer to reload approvals state
            pollingVerificationTimer.startWithDelay(
                initialDelay = CountDownTimerImpl.Companion.INITIAL_DELAY,
                interval = CountDownTimerImpl.Companion.POLLING_VERIFICATION_COUNTDOWN
            ) {
                if (state.userResponse !is Resource.Loading) {
                    retrieveOwnerState(silent = true)
                }
            }
        }
    }

    fun onStop() {
        pollingVerificationTimer.stop()
    }

    fun retrieveOwnerState(silent: Boolean = false) {
        if (!silent) {
            state = state.copy(userResponse = Resource.Loading())
        }

        viewModelScope.launch {
            val ownerStateResource = ownerRepository.retrieveUser().map { it.ownerState }

            ownerStateResource.data?.let { updateOwnerState(it) }

            state = state.copy(userResponse = ownerStateResource)
        }
    }

    private fun updateOwnerState(ownerState: OwnerState) {
        if (ownerState !is OwnerState.Ready) {
            // other owner states are not supported on this view
            // navigate back to start of the app so it can fix itself
            state = state.copy(navigationResource = Resource.Success(Screen.EntranceRoute.route))
            return
        }

        // update global state
        ownerStateFlow.tryEmit(Resource.Success(ownerState))

        // restore state
        when (val recovery = ownerState.recovery) {
            null -> {
                state = state.copy(initiateNewRecovery = true)
            }

            is Recovery.AnotherDevice -> {
                state = state.copy(
                    accessApprovalUIState = AccessApprovalUIState.AnotherDevice
                )
            }

            is Recovery.ThisDevice -> {
                state = state.copy(
                    recovery = recovery,
                    approvals = recovery.approvals,
                    approvers = ownerState.policy.guardians
                )

                if (recovery.status == RecoveryStatus.Available) {
                    if (ownerState.policy.guardians.all { it.isOwner }) {
                        // owner is single approver, skip to view seed phrases
                        navigateToViewSeedPhrases()
                    } else {
                        state = state.copy(accessApprovalUIState = AccessApprovalUIState.Approved)
                    }
                } else if (state.accessApprovalUIState == AccessApprovalUIState.Initial) {
                    // determine initial UI state
                    state = state.copy(accessApprovalUIState = AccessApprovalUIState.SelectApprover)
                } else {
                    checkForRejections(recovery)
                }
            }
        }
    }

    private fun checkForRejections(recovery: Recovery.ThisDevice) {
        recovery.approvals.find { it.participantId == state.selectedApprover?.participantId }
            ?.let {
                if (state.waitingForApproval && it.status == ApprovalStatus.Rejected) {
                    state = state.copy(
                        waitingForApproval = false,
                        verificationCode = ""
                    )
                }
            }
    }

    fun initiateRecovery() {
        if (state.initiateRecoveryResource !is Resource.Loading) {
            state = state.copy(initiateRecoveryResource = Resource.Loading())

            viewModelScope.launch {
                val response = ownerRepository.initiateRecovery(RecoveryIntent.AccessPhrases)

                if (response is Resource.Success) {
                    updateOwnerState(response.data!!.ownerState)
                }

                state = state.copy(
                    initiateRecoveryResource = response,
                    initiateNewRecovery = false
                )
            }
        }
    }

    fun cancelAccess() {
        state = state.copy(
            showCancelConfirmationDialog = false,
            cancelRecoveryResource = Resource.Loading()
        )

        viewModelScope.launch {
            val response = ownerRepository.cancelRecovery()

            if (response is Resource.Success) {
                state = state.copy(
                    recovery = null,
                    navigationResource = Resource.Success(Screen.OwnerVaultScreen.route)
                )
            }

            state = state.copy(cancelRecoveryResource = response)
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
            submitTotpVerificationResource = Resource.Loading(),
            waitingForApproval = true
        )

        viewModelScope.launch {
            val submitVerificationResource = ownerRepository.submitRecoveryTotpVerification(
                participantId = participantId,
                verificationCode = verificationCode
            )

            if (submitVerificationResource is Resource.Success) {
                updateOwnerState(submitVerificationResource.map { it.ownerState }.data!!)
            }

            state = state.copy(submitTotpVerificationResource = submitVerificationResource)
        }
    }

    fun resetNavigationResource() {
        state = state.copy(
            navigationResource = Resource.Uninitialized
        )
    }


    fun onApproverSelected(selectedApprover: Guardian.TrustedGuardian) {
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

    fun navigateToViewSeedPhrases() {
        state = state.copy(navigationResource = Resource.Success(Screen.AccessSeedPhrases.route))
    }

    fun hideCloseConfirmationDialog() {
        state = state.copy(showCancelConfirmationDialog = false)
    }
}

internal fun List<Guardian.TrustedGuardian>.external(): List<Guardian.TrustedGuardian> {
    return filter { !it.isOwner }
}

internal fun List<Guardian.TrustedGuardian>.primaryApprover(): Guardian.TrustedGuardian? {
    return external().minByOrNull { it.attributes.onboardedAt }
}

internal fun List<Guardian.TrustedGuardian>.backupApprover(): Guardian.TrustedGuardian? {
    val externalApprovers = external()

    return when {
        externalApprovers.isEmpty() -> null
        externalApprovers.size == 1 -> null
        else -> externalApprovers.maxBy { it.attributes.onboardedAt }
    }
}

internal fun Guardian.TrustedGuardian?.isDefined(): Boolean {
    return this != null
}