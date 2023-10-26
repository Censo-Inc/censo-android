package co.censo.vault.presentation.access_approval

import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.SharedScreen
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.model.Approval
import co.censo.shared.data.model.ApprovalStatus
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.Recovery
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.CountDownTimerImpl
import co.censo.shared.util.VaultCountDownTimer
import co.censo.vault.presentation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccessApprovalViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val pollingVerificationTimer: VaultCountDownTimer
) : ViewModel() {

    var state by mutableStateOf(AccessApprovalState())
        private set

    fun onStart() {
        reloadOwnerState()

        // setup polling timer to reload approvals state
        pollingVerificationTimer.startCountDownTimer(CountDownTimerImpl.Companion.POLLING_VERIFICATION_COUNTDOWN) {
            if (state.userResponse !is Resource.Loading) {
                reloadOwnerState(silent = true)
            }
        }
    }

    fun onStop() {
        pollingVerificationTimer.stopCountDownTimer()
    }

    fun reloadOwnerState(silent: Boolean = false) {
        if (!silent) {
            state = state.copy(userResponse = Resource.Loading())
        }

        viewModelScope.launch {
            val response = ownerRepository.retrieveUser()

            state = state.copy(userResponse = response)

            if (response is Resource.Success) {
                onOwnerState(response.data!!.ownerState)
            }
        }
    }

    private fun onOwnerState(ownerState: OwnerState) {
        when (ownerState) {
            is OwnerState.Ready -> {
                state = state.copy(
                    initiateNewRecovery = ownerState.recovery == null,
                    recovery = ownerState.recovery,
                    approvers = ownerState.policy.guardians,
                    approvals = (ownerState.recovery as Recovery.ThisDevice).approvals,
                    secrets = ownerState.vault.secrets.map { it.guid },
                )

                determineCodeVerificationState(ownerState)
            }

            else -> {
                // other owner states are not supported on this view
                // navigate back to start of the app so it can fix itself
                state = state.copy(
                    navigationResource = Resource.Success(SharedScreen.EntranceRoute.route)
                )
            }
        }
    }

    private fun determineCodeVerificationState(ownerState: OwnerState.Ready) {
        // navigate out of verification code screen once approved
        val recovery = ownerState.recovery
        val totpVerificationState = state.totpVerificationState

        if (totpVerificationState.showModal && recovery is Recovery.ThisDevice) {
            recovery.approvals.find { it.participantId == totpVerificationState.participantId }
                ?.let { approval ->
                    if (approval.status == ApprovalStatus.Approved) {
                        state = state.copy(
                            totpVerificationState = totpVerificationState.copy(
                                showModal = false
                            )
                        )
                    } else if (totpVerificationState.waitingForApproval && approval.status == ApprovalStatus.Rejected) {
                        state = state.copy(
                            totpVerificationState = totpVerificationState.copy(
                                verificationCode = "",
                                waitingForApproval = false,
                                rejected = true,
                            )
                        )
                    }
                }
        }
    }

    fun reset() {
        state = AccessApprovalState()
    }

    fun initiateRecovery() {
        state = state.copy(
            initiateNewRecovery = false,
            initiateRecoveryResource = Resource.Loading()
        )
        viewModelScope.launch {
            val response = ownerRepository.initiateRecovery(state.secrets)

            state = state.copy(
                initiateRecoveryResource = response
            )

            if (response is Resource.Success) {
                onOwnerState(response.data!!.ownerState)
            }
        }
    }

    fun cancelRecovery() {
        state = state.copy(
            cancelRecoveryResource = Resource.Loading()
        )

        viewModelScope.launch {
            val response = ownerRepository.cancelRecovery()

            state = state.copy(
                cancelRecoveryResource = response
            )

            if (response is Resource.Success) {
                state = state.copy(
                    recovery = null,
                    navigationResource = Resource.Success(SharedScreen.OwnerVaultScreen.route)
                )
            }
        }
    }

    fun showCodeEntryModal(approval: Approval) {
        val guardian = state.approvers.find { it.participantId == approval.participantId }!!
        state = state.copy(
            // reset verification state on re-entry
            totpVerificationState = TotpVerificationScreenState(
                showModal = true,

                approverLabel = guardian.label,
                participantId = approval.participantId,
            )
        )
    }

    fun updateVerificationCode(participantId: ParticipantId, code: String) {
        if (state.submitTotpVerificationResource is Resource.Error) {
            state = state.copy(
                submitTotpVerificationResource = Resource.Uninitialized,
            )
        }

        if (code.isDigitsOnly()) {
            state = state.copy(
                totpVerificationState = state.totpVerificationState.copy(
                    verificationCode = code,
                    rejected = false,
                    waitingForApproval = false
                )
            )

            if (code.length == TotpGenerator.CODE_LENGTH) {
                submitVerificationCode(participantId, code)
            }
        }
    }

    private fun submitVerificationCode(participantId: ParticipantId, verificationCode: String) {
        state = state.copy(submitTotpVerificationResource = Resource.Loading())

        viewModelScope.launch {
            val submitVerificationResource = ownerRepository.submitRecoveryTotpVerification(
                participantId = participantId,
                verificationCode = verificationCode
            )

            state = state.copy(
                submitTotpVerificationResource = submitVerificationResource,
            )

            if (submitVerificationResource is Resource.Success) {
                state = state.copy(
                    submitTotpVerificationResource = submitVerificationResource,
                    totpVerificationState = state.totpVerificationState.copy(
                        waitingForApproval = true
                    )
                )
            }
        }
    }

    fun dismissVerification() {
        state = state.copy(
            totpVerificationState = state.totpVerificationState.copy(
                showModal = false
            )
        )
    }

    fun onRecoverPhrases() {
        state = state.copy(
            navigationResource = Resource.Success(Screen.AccessSeedPhrases.route)
        )
    }

    fun onResumeLater() {
        state = state.copy(
            navigationResource = Resource.Success(SharedScreen.OwnerVaultScreen.route)
        )
    }

    fun resetNavigationResource() {
        state = state.copy(
            navigationResource = Resource.Uninitialized
        )
    }

    fun onContinueLive() {
        state = if (state.approvers.backupApprover().isDefined()) {
            state.copy(
                accessApprovalUIState = AccessApprovalUIState.SelectApprover
            )
        } else {
            state.copy(
                selectedApprover = state.approvers.primaryApprover(),
                accessApprovalUIState = AccessApprovalUIState.ApproveAccess
            )
        }
    }

    fun onApproverSelected(selectedApprover: Guardian.TrustedGuardian) {
        state = if (state.selectedApprover == selectedApprover) {
            state.copy(selectedApprover = null)
        } else {
            state.copy(selectedApprover = selectedApprover)
        }
    }

    fun onVerificationCodeChanged(verificationCode: String) {
        state = state.copy(verificationCode = verificationCode)
        //FIXME submit automatically
    }

    fun continueToApproveAccess() {
        state = state.copy(accessApprovalUIState = AccessApprovalUIState.ApproveAccess)
    }

    fun onBackClicked() {
        when (state.accessApprovalUIState) {
            AccessApprovalUIState.GettingLive -> {
                state = state.copy(navigationResource = Resource.Success(SharedScreen.OwnerVaultScreen.route))
            }

            AccessApprovalUIState.SelectApprover -> {
                state = state.copy(accessApprovalUIState = AccessApprovalUIState.GettingLive)
            }

            AccessApprovalUIState.ApproveAccess -> {
                state = state.copy(accessApprovalUIState = AccessApprovalUIState.SelectApprover)
            }

            AccessApprovalUIState.Approved -> {
                state = state.copy(accessApprovalUIState = AccessApprovalUIState.ApproveAccess)
            }
        }
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