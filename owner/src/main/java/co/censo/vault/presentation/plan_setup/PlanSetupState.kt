package co.censo.vault.presentation.plan_setup

import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.model.CreatePolicySetupApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.InitiateRecoveryApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.ReplacePolicyApiResponse
import co.censo.shared.data.model.RetrieveRecoveryShardsApiResponse
import kotlinx.datetime.Clock


data class PlanSetupState(
    val ownerState: OwnerState.Ready? = null,

    // restored approvers state
    val ownerApprover: Guardian.ProspectGuardian? = null,
    val primaryApprover: Guardian.ProspectGuardian? = null,
    val alternateApprover: Guardian.ProspectGuardian? = null,

    // Screen in Plan Setup Flow
    val planSetupUIState: PlanSetupUIState = PlanSetupUIState.Initial,

    // inviting approver
    val editedNickname: String = "",

    // totp
    val secondsLeft: Int = 0,
    val counter: Long = Clock.System.now().epochSeconds.div(TotpGenerator.CODE_EXPIRATION),
    val approverCodes: Map<ParticipantId, String> = emptyMap(),


    // API Calls
    val userResponse: Resource<OwnerState> = Resource.Uninitialized,
    val createPolicySetupResponse: Resource<CreatePolicySetupApiResponse> = Resource.Uninitialized,
    val initiateRecoveryResponse: Resource<InitiateRecoveryApiResponse> = Resource.Uninitialized,
    val retrieveRecoveryShardsResponse: Resource<RetrieveRecoveryShardsApiResponse> = Resource.Uninitialized,
    val replacePolicyResponse: Resource<ReplacePolicyApiResponse> = Resource.Uninitialized,

    // Navigation
    val navigationResource: Resource<String> = Resource.Uninitialized
) {

    val backArrowType = when (planSetupUIState) {
        PlanSetupUIState.ApproverActivation,
        PlanSetupUIState.EditApproverNickname -> BackIconType.Back

        PlanSetupUIState.InviteApprovers,
        PlanSetupUIState.ApproverNickname,
        PlanSetupUIState.ApproverGettingLive,
        PlanSetupUIState.AddAlternateApprover,
        PlanSetupUIState.RecoveryInProgress -> BackIconType.Exit

        PlanSetupUIState.Initial, PlanSetupUIState.Completed -> BackIconType.None
    }

    val activatingApprover = alternateApprover ?: primaryApprover
    val approverType = if (alternateApprover != null) ApproverType.Alternate else ApproverType.Primary

    val loading = userResponse is Resource.Loading
                || createPolicySetupResponse is Resource.Loading
                || initiateRecoveryResponse is Resource.Loading
                || retrieveRecoveryShardsResponse is Resource.Loading
                || replacePolicyResponse is Resource.Loading

    val asyncError = userResponse is Resource.Error
            || createPolicySetupResponse is Resource.Error
            || initiateRecoveryResponse is Resource.Error
            || retrieveRecoveryShardsResponse is Resource.Error
            || replacePolicyResponse is Resource.Error

    enum class BackIconType {
        None, Back, Exit
    }
}

enum class PlanSetupUIState {
    Initial,
    InviteApprovers,
    ApproverNickname,
    EditApproverNickname,
    ApproverGettingLive,
    ApproverActivation,
    AddAlternateApprover,
    RecoveryInProgress,
    Completed
}

enum class ApproverType {
    Primary, Alternate
}