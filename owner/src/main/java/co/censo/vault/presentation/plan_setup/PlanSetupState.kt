package co.censo.vault.presentation.plan_setup

import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.model.CreatePolicySetupApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.data.model.OwnerState
import kotlinx.datetime.Clock


data class PlanSetupState(
    val ownerState: OwnerState.Ready? = null,
    // Screen in Plan Setup Flow
    val planSetupUIState: PlanSetupUIState = PlanSetupUIState.InviteApprovers,
    val guardians: List<Guardian.ProspectGuardian> = emptyList(),

    val secondsLeft: Int = 0,
    val counter: Long = Clock.System.now().epochSeconds.div(TotpGenerator.CODE_EXPIRATION),
    val approverCodes: Map<ParticipantId, String> = emptyMap(),

    val editedNickname: String = "",

    // API Calls
    val userResponse: Resource<OwnerState> = Resource.Uninitialized,
    val createPolicySetupResponse: Resource<CreatePolicySetupApiResponse> = Resource.Uninitialized,

    val createPolicyResponse: Resource<CreatePolicySetupApiResponse> = Resource.Uninitialized,

    // Navigation
    val navigationResource: Resource<String> = Resource.Uninitialized
) {

    val backArrowType = when (planSetupUIState) {
        PlanSetupUIState.InviteApprovers,
        PlanSetupUIState.ApproverActivation -> BackIconType.Back

        PlanSetupUIState.PrimaryApproverNickname,
        PlanSetupUIState.PrimaryApproverGettingLive,
        PlanSetupUIState.AddBackupApprover,
        PlanSetupUIState.BackupApproverNickname,
        PlanSetupUIState.BackupApproverGettingLive,
        PlanSetupUIState.Completed -> BackIconType.Exit
    }

    val activatingApprover = guardians.firstOrNull {
        it.status !is GuardianStatus.ImplicitlyOwner
                && it.status !is GuardianStatus.Confirmed
                && it.status !is GuardianStatus.Onboarded
    }

    val loading =
        userResponse is Resource.Loading || createPolicySetupResponse is Resource.Loading

    val asyncError =
        userResponse is Resource.Error || createPolicySetupResponse is Resource.Error

    //Fixme: this is not nice code!
    val approverType = if (guardians.size <= 2) {
        ApproverType.Primary
    } else {
        ApproverType.Backup
    }

    enum class BackIconType {
        Back, Exit
    }
}

enum class PlanSetupUIState {
    InviteApprovers,
    PrimaryApproverNickname,
    PrimaryApproverGettingLive,
    AddBackupApprover,
    BackupApproverNickname,
    BackupApproverGettingLive,
    ApproverActivation,
    Completed
}

enum class ApproverType {
    Primary, Backup
}