package co.censo.vault.presentation.access_approval

import ParticipantId
import VaultSecretId
import co.censo.shared.data.Resource
import co.censo.shared.data.model.DeleteRecoveryApiResponse
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.InitiateRecoveryApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.Recovery
import co.censo.shared.data.model.SubmitRecoveryTotpVerificationApiResponse

data class TotpVerificationScreenState(
    val verificationCode: String = "",
    val waitingForApproval: Boolean = false,
    val rejected: Boolean = false,
    val showModal: Boolean = false,
    val approverLabel: String = "",
    val participantId: ParticipantId = ParticipantId("")
)

data class AccessApprovalState(
    // UI state
    val accessApprovalUIState: AccessApprovalUIState = AccessApprovalUIState.GettingLive,
    val selectedApprover: Guardian.TrustedGuardian? = null,

    // data
    val ownerState: Resource<OwnerState> = Resource.Uninitialized,
    val approvers: List<Guardian.TrustedGuardian> = listOf(),


    // owner state
    val secrets: List<VaultSecretId> = listOf(),
    val recovery: Recovery? = null,
    val approvalsRequired: Int = 0,

    // recovery control
    val initiateNewRecovery: Boolean = false,

    // totp code verification
    val totpVerificationState: TotpVerificationScreenState = TotpVerificationScreenState(),

    // api requests
    val userResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
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
    GettingLive, SelectApprover, ApproveAccess, Approved
}

