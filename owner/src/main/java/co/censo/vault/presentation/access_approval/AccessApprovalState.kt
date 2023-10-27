package co.censo.vault.presentation.access_approval

import co.censo.shared.data.Resource
import co.censo.shared.data.model.Approval
import co.censo.shared.data.model.DeleteRecoveryApiResponse
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.InitiateRecoveryApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.Recovery
import co.censo.shared.data.model.SubmitRecoveryTotpVerificationApiResponse

data class AccessApprovalState(
    // UI state
    val accessApprovalUIState: AccessApprovalUIState = AccessApprovalUIState.GettingLive,
    val selectedApprover: Guardian.TrustedGuardian? = null,
    val verificationCode: String = "",
    val waitingForApproval: Boolean = false,

    // data
    val ownerState: OwnerState.Ready? = null,
    val recovery: Recovery.ThisDevice? = null,
    val approvers: List<Guardian.TrustedGuardian> = listOf(),
    val approvals: List<Approval> = listOf(),

    // recovery control
    val initiateNewRecovery: Boolean = false,

    // api requests
    val userResponse: Resource<OwnerState> = Resource.Uninitialized,
    val approvalsResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val initiateRecoveryResource: Resource<InitiateRecoveryApiResponse> = Resource.Uninitialized,
    val cancelRecoveryResource: Resource<DeleteRecoveryApiResponse> = Resource.Uninitialized,
    val submitTotpVerificationResource: Resource<SubmitRecoveryTotpVerificationApiResponse> = Resource.Uninitialized,

    // navigation
    val navigationResource: Resource<String> = Resource.Uninitialized,
) {

    val loading = userResponse is Resource.Loading
            || initiateRecoveryResource is Resource.Loading
            || cancelRecoveryResource is Resource.Loading
            || submitTotpVerificationResource is Resource.Loading

    val asyncError = userResponse is Resource.Error
            || initiateRecoveryResource is Resource.Error
            || cancelRecoveryResource is Resource.Error
            || submitTotpVerificationResource is Resource.Loading
}

enum class AccessApprovalUIState {
    AnotherDevice, GettingLive, SelectApprover, ApproveAccess, Approved
}
