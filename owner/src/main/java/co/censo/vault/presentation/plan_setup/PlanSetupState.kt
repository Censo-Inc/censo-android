package co.censo.vault.presentation.plan_setup

import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.model.CreatePolicySetupApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.data.model.OwnerState
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


data class PlanSetupState(
    val ownerState: OwnerState? = null,
    // Screen in Plan Setup Flow
    val planSetupUIState: PlanSetupUIState = PlanSetupUIState.InviteApprovers,
    val counter: Long = Clock.System.now().epochSeconds.div(TotpGenerator.CODE_EXPIRATION),
    val guardians: List<Guardian.ProspectGuardian> = emptyList(),
    val approverCodes: Map<ParticipantId, String> = emptyMap(),

    val currentSecond: Int = Clock.System.now().toLocalDateTime(TimeZone.UTC).second,

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

    val activatingApprover = guardians.firstOrNull { it.label != "Me" && it.status !is GuardianStatus.Confirmed }

    val loading =
        userResponse is Resource.Loading || createPolicySetupResponse is Resource.Loading

    val asyncError =
        userResponse is Resource.Error || createPolicySetupResponse is Resource.Error

    //Fixme: this is not nice code!
    val approverType =
        if (guardians.size < 2) {
            ApproverType.Primary
        } else if (guardians.size == 3) {
            ApproverType.Backup
        } else {
            val nonOwnerGuardian = guardians.first { it.label != "me" }
            if (nonOwnerGuardian.status is GuardianStatus.Confirmed) {
                ApproverType.Backup
            } else {
                ApproverType.Primary
            }
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