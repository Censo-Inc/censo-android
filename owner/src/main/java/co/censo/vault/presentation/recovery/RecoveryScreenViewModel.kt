package co.censo.vault.presentation.recovery

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
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.Recovery
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.CountDownTimerImpl
import co.censo.shared.util.VaultCountDownTimer
import co.censo.vault.presentation.home.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecoveryScreenViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val pollingVerificationTimer: VaultCountDownTimer
) : ViewModel() {

    var state by mutableStateOf(RecoveryScreenState())
        private set

    fun onStart() {

        reloadOwnerState()

        // setup polling timer to reload approvals state
        pollingVerificationTimer.startCountDownTimer(CountDownTimerImpl.Companion.POLLING_VERIFICATION_COUNTDOWN) {
            if (state.userResponse !is Resource.Loading) {
                silentReloadUserState()
            }
        }
    }

    fun onStop() {
        pollingVerificationTimer.stopCountDownTimer()
    }

    private fun silentReloadUserState() {
        viewModelScope.launch {
            val response = ownerRepository.retrieveUser()
            if (response is Resource.Success) {
                onOwnerState(response.data!!.ownerState)
            }
        }
    }

    fun reloadOwnerState() {
        state = state.copy(userResponse = Resource.Loading())

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
                    initiateNewRecovery = ownerState.policy.recovery == null,
                    recovery = ownerState.policy.recovery,
                    guardians = ownerState.policy.guardians,
                    secrets = ownerState.vault.secrets.map { it.guid },
                    approvalsCollected = ownerState.policy.recovery?.let {
                        when (it) {
                            is Recovery.ThisDevice -> it.approvals.count { it.status == ApprovalStatus.Approved }
                            else -> 0
                        }
                    } ?: 0,
                    approvalsRequired = ownerState.policy.threshold.toInt()
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
        val screen = state.recoveryUIState
        val recovery = ownerState.policy.recovery

        if (screen is RecoveryUIState.EnterVerificationCodeState && recovery is Recovery.ThisDevice) {
            recovery.approvals.find { it.participantId == screen.participantId }
                ?.let { approval ->
                    if (approval.status == ApprovalStatus.Approved) {
                        state = state.copy(
                            recoveryUIState = RecoveryUIState.Main
                        )
                    } else if (state.totpVerificationWaitingForApproval && approval.status == ApprovalStatus.Rejected) {
                        state = state.copy(
                            totpVerificationCode = "",
                            totpVerificationWaitingForApproval = false,
                            totpVerificationRejected = true,
                        )
                    }
                }
        }
    }

    fun reset() {
        state = RecoveryScreenState()
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
                    navigationResource = Resource.Success(Screen.VaultScreen.route)
                )
            }
        }
    }

    fun onProceedWithVerification(approval: Approval) {
        val guardian = state.guardians.find { it.participantId == approval.participantId }!!
        state = state.copy(
            recoveryUIState = RecoveryUIState.EnterVerificationCodeState(
                approverLabel = guardian.label,
                participantId = approval.participantId
            ),

            // update verification state on re-entry
            totpVerificationCode = "",
            totpVerificationWaitingForApproval = approval.status == ApprovalStatus.WaitingForApproval,
            totpVerificationRejected = false
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
                totpVerificationCode = code,
                totpVerificationRejected = false,
                totpVerificationWaitingForApproval = false
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
                    totpVerificationWaitingForApproval = true,
                )
            }
        }
    }

    fun dismissVerification() {
        state = state.copy(
            recoveryUIState = RecoveryUIState.Main
        )
    }

}
