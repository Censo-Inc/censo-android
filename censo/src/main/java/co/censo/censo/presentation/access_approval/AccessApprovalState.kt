package co.censo.censo.presentation.access_approval

import co.censo.shared.data.Resource
import co.censo.shared.data.model.AccessApproval
import co.censo.shared.data.model.DeleteAccessApiResponse
import co.censo.shared.data.model.GetOwnerUserApiResponse
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.InitiateAccessApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.Access
import co.censo.shared.data.model.AccessIntent
import co.censo.shared.data.model.SubmitAccessTotpVerificationApiResponse
import co.censo.shared.util.NavigationData

data class AccessApprovalState(
    // UI state
    val accessApprovalUIState: AccessApprovalUIState = AccessApprovalUIState.Initial,
    val selectedApprover: Approver.TrustedApprover? = null,
    val verificationCode: String = "",
    val waitingForApproval: Boolean = false,
    val showCancelConfirmationDialog: Boolean = false,

    // data
    val accessIntent: AccessIntent = AccessIntent.AccessPhrases,
    val ownerState: OwnerState.Ready? = null,
    val access: Access.ThisDevice? = null,
    val approvers: List<Approver.TrustedApprover> = listOf(),
    val approvals: List<AccessApproval> = listOf(),

    // access control
    val initiateNewAccess: Boolean = false,

    // api requests
    val userResponse: Resource<OwnerState> = Resource.Uninitialized,
    val approvalsResponse: Resource<GetOwnerUserApiResponse> = Resource.Uninitialized,
    val initiateAccessResource: Resource<InitiateAccessApiResponse> = Resource.Uninitialized,
    val cancelAccessResource: Resource<DeleteAccessApiResponse> = Resource.Uninitialized,
    val submitTotpVerificationResource: Resource<SubmitAccessTotpVerificationApiResponse> = Resource.Uninitialized,

    // navigation
    val navigationResource: Resource<NavigationData> = Resource.Uninitialized,
    val isTimelocked: Boolean = false
) {

    val loading = userResponse is Resource.Loading
            || initiateAccessResource is Resource.Loading
            || cancelAccessResource is Resource.Loading
            || submitTotpVerificationResource is Resource.Loading

    val asyncError = userResponse is Resource.Error
            || initiateAccessResource is Resource.Error
            || cancelAccessResource is Resource.Error
            || submitTotpVerificationResource is Resource.Loading
}

enum class AccessApprovalUIState {
    Initial, AnotherDevice, SelectApprover, ApproveAccess, Approved
}
