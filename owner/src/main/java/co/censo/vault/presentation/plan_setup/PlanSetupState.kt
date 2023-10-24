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
    val backupApprover: Guardian.ProspectGuardian? = null,

    // Screen in Plan Setup Flow
    val planSetupUIState: PlanSetupUIState = PlanSetupUIState.InviteApprovers,

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
        PlanSetupUIState.InviteApprovers,
        PlanSetupUIState.ApproverActivation -> BackIconType.Back

        PlanSetupUIState.ApproverNickname,
        PlanSetupUIState.ApproverGettingLive,
        PlanSetupUIState.AddBackupApprover,
        PlanSetupUIState.RecoveryInProgress,
        PlanSetupUIState.Completed -> BackIconType.Exit

    }

    val activatingApprover = backupApprover ?: primaryApprover
    val approverType = if (backupApprover != null) ApproverType.Backup else ApproverType.Primary

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
        Back, Exit
    }
}

enum class PlanSetupUIState {
    InviteApprovers,
    ApproverNickname,
    ApproverGettingLive,
    ApproverActivation,
    AddBackupApprover,
    RecoveryInProgress,
    Completed
}

enum class ApproverType {
    Primary, Backup
}