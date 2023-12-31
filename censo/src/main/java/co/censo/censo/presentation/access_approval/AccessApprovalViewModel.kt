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
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.Access
import co.censo.shared.data.model.AccessIntent
import co.censo.shared.data.model.AccessStatus
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

    fun onStart(accessIntent: AccessIntent) {
        state = state.copy(accessIntent = accessIntent)

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
        when (val access = ownerState.access) {
            null -> {
                state = state.copy(initiateNewAccess = true)
            }

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
                    if (ownerState.policy.approvers.all { it.isOwner }) {
                        // owner is single approver, skip to view seed phrases
                        navigateIntentAware()
                    } else {
                        state = state.copy(accessApprovalUIState = AccessApprovalUIState.Approved)
                    }
                } else if (state.accessApprovalUIState == AccessApprovalUIState.Initial) {
                    // determine initial UI state
                    state = state.copy(accessApprovalUIState = AccessApprovalUIState.SelectApprover)
                } else {
                    checkForRejections(access)
                }
            }
        }
    }

    private fun checkForRejections(access: Access.ThisDevice) {
        access.approvals.find { it.participantId == state.selectedApprover?.participantId }
            ?.let {
                if (state.waitingForApproval && it.status == ApprovalStatus.Rejected) {
                    state = state.copy(
                        waitingForApproval = false,
                        verificationCode = ""
                    )
                }
            }
    }

    fun initiateAccess() {
        if (state.initiateAccessResource !is Resource.Loading) {
            state = state.copy(initiateAccessResource = Resource.Loading())

            viewModelScope.launch {
                val response = ownerRepository.initiateAccess(state.accessIntent)

                if (response is Resource.Success) {
                    updateOwnerState(response.data!!.ownerState)
                }

                state = state.copy(
                    initiateAccessResource = response,
                    initiateNewAccess = false
                )
            }
        }
    }

    fun cancelAccess() {
        state = state.copy(
            showCancelConfirmationDialog = false,
            cancelAccessResource = Resource.Loading()
        )

        viewModelScope.launch {
            val response = ownerRepository.cancelAccess()

            if (response is Resource.Success) {
                state = state.copy(
                    access = null,
                    navigationResource = Resource.Success(Screen.OwnerVaultScreen.route)
                )
            }

            state = state.copy(cancelAccessResource = response)
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
            val submitVerificationResource = ownerRepository.submitAccessTotpVerification(
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
        }

        state = state.copy(navigationResource = Resource.Success(destination))
    }

    fun hideCloseConfirmationDialog() {
        state = state.copy(showCancelConfirmationDialog = false)
    }
}

internal fun List<Approver.TrustedApprover>.external(): List<Approver.TrustedApprover> {
    return filter { !it.isOwner }
}

internal fun List<Approver.TrustedApprover>.primaryApprover(): Approver.TrustedApprover? {
    return external().minByOrNull { it.attributes.onboardedAt }
}

internal fun List<Approver.TrustedApprover>.backupApprover(): Approver.TrustedApprover? {
    val externalApprovers = external()

    return when {
        externalApprovers.isEmpty() -> null
        externalApprovers.size == 1 -> null
        else -> externalApprovers.maxBy { it.attributes.onboardedAt }
    }
}

internal fun Approver.TrustedApprover?.isDefined(): Boolean {
    return this != null
}