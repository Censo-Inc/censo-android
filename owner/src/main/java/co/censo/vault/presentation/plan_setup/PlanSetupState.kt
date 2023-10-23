package co.censo.vault.presentation.plan_setup

import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.model.CreatePolicySetupApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.data.model.InitiateRecoveryApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.ReplacePolicyApiResponse
import co.censo.shared.data.model.RetrieveRecoveryShardsApiResponse
import kotlinx.datetime.Clock


data class PlanSetupState(
    val ownerState: OwnerState.Ready? = null,
    // Screen in Plan Setup Flow
    val planSetupUIState: PlanSetupUIState = PlanSetupUIState.InviteApprovers,
    val setupApprovers: List<Guardian.ProspectGuardian> = emptyList(),

    val secondsLeft: Int = 0,
    val counter: Long = Clock.System.now().epochSeconds.div(TotpGenerator.CODE_EXPIRATION),
    val approverCodes: Map<ParticipantId, String> = emptyMap(),

    val editedNickname: String = "",

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
        PlanSetupUIState.ReShardingSecrets,
        PlanSetupUIState.Completed -> BackIconType.Exit

    }

    val activatingApprover = setupApprovers.firstOrNull {
        it.status !is GuardianStatus.ImplicitlyOwner
                && it.status !is GuardianStatus.Confirmed
                && it.status !is GuardianStatus.Onboarded
    }

    //Fixme: this is not nice code!
    val approverType = if (setupApprovers.size <= 2) {
        ApproverType.Primary
    } else {
        ApproverType.Backup
    }


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
    ReShardingSecrets,
    Completed
}

enum class ApproverType {
    Primary, Backup
}