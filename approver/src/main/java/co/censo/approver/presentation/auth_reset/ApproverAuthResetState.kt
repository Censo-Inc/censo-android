package co.censo.approver.presentation.auth_reset

import Base64EncodedData
import ParticipantId
import co.censo.approver.presentation.home.EncryptedKey
import co.censo.shared.data.Resource
import co.censo.shared.data.model.AcceptAuthenticationResetRequestApiResponse
import co.censo.shared.data.model.GetApproverUserApiResponse
import co.censo.shared.data.model.ApproverPhase
import co.censo.shared.data.model.ApproverState
import co.censo.shared.data.model.AuthenticationResetApprovalId
import co.censo.shared.data.model.SubmitAuthenticationResetTotpVerificationApiResponse
import co.censo.shared.presentation.cloud_storage.CloudStorageActionData

data class ApproverAuthResetState(
    // approver state
    val approverState: ApproverState? = null,

    // deep links data
    val approvalId: AuthenticationResetApprovalId = AuthenticationResetApprovalId(""),
    val participantId: ParticipantId = ParticipantId(""),

    // biometry reset approval
    val verificationCode: String = "",
    val approverEncryptedKey: EncryptedKey? = null,
    val approverEntropy: Base64EncodedData? = null,
    val userResponse: Resource<GetApproverUserApiResponse> = Resource.Uninitialized,
    val acceptAuthResetResource: Resource<AcceptAuthenticationResetRequestApiResponse> = Resource.Uninitialized,
    val submitAuthResetVerificationResource: Resource<SubmitAuthenticationResetTotpVerificationApiResponse> = Resource.Uninitialized,
    val authResetNotInProgress: Resource<Unit> = Resource.Uninitialized,

    // UI state
    val uiState: UIState = UIState.AuthenticationResetRequested,
    val showTopBarCancelConfirmationDialog: Boolean = false,
    val navToApproverEntrance: Boolean = false,

    // Cloud Storage
    val cloudStorageAction: CloudStorageActionData = CloudStorageActionData(),
    val loadKeyFromCloudResource: Resource<Unit> = Resource.Uninitialized,
    ) {

    val loading = userResponse is Resource.Loading
            || acceptAuthResetResource is Resource.Loading
            || submitAuthResetVerificationResource is Resource.Loading

    val asyncError = userResponse is Resource.Error
            || acceptAuthResetResource is Resource.Error
            || submitAuthResetVerificationResource is Resource.Error
            || authResetNotInProgress is Resource.Error

    val showTopBar = (!loading && !asyncError) && uiState != UIState.Complete

    val shouldAutoSubmitCode =
        approverState?.phase is ApproverPhase.AuthenticationResetWaitingForCode ||
            approverState?.phase is ApproverPhase.AuthenticationResetVerificationRejected

    enum class UIState {
        AuthenticationResetRequested, NeedsToEnterCode, WaitingForVerification, CodeRejected, Complete
    }
}

