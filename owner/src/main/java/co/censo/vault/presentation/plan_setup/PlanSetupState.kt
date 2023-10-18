package co.censo.vault.presentation.plan_setup

import co.censo.shared.data.Resource
import co.censo.shared.data.model.CreatePolicySetupApiResponse
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.vault.presentation.components.security_plan.SetupSecurityPlanScreen
import co.censo.vault.presentation.activate_approvers.ActivateApproversViewModel.Companion.MIN_GUARDIAN_LIMIT
import co.censo.vault.presentation.enter_phrase.BackIconType
import co.censo.vault.presentation.enter_phrase.EnterPhraseUIState

data class PlanSetupState(
    //Screen in Plan Setup Flow
    val currentScreen: SetupSecurityPlanScreen = SetupSecurityPlanScreen.Initial,
    val planSetupUIState: PlanSetupUIState = PlanSetupUIState.InviteApprovers,

    //In Progress UI Data
    val navigateToActivateApprovers: Boolean = false,
/*    val addedApproverNickname: String = "",
    val editingGuardian: Guardian.SetupGuardian.ExternalApprover? = null,*/

    //Dialog Triggers
/*    val showAddGuardianDialog: Boolean = false,
    val showEditOrDeleteDialog: Boolean = false,
    val showCancelPlanSetupDialog: Boolean = false,*/

    //Plan Data
    val primaryApproverNickname: String = "",
    val primaryApprover: Guardian.SetupGuardian.ExternalApprover? = null,

    val guardians: List<Guardian.SetupGuardian.ExternalApprover> = emptyList(),
    val threshold: UInt = 1u,


    //API Calls
    val userResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val createPolicySetupResponse: Resource<CreatePolicySetupApiResponse> = Resource.Uninitialized,
) {

    val backArrowType = when (planSetupUIState) {
        PlanSetupUIState.InviteApprovers,
        PlanSetupUIState.PrimaryApproverActivation,
        PlanSetupUIState.BackupApproverActivation -> BackIconType.BACK

        PlanSetupUIState.PrimaryApproverNickname,
        PlanSetupUIState.PrimaryApproverGettingLive,
        PlanSetupUIState.AddBackupApprover,
        PlanSetupUIState.BackupApproverNickname,
        PlanSetupUIState.BackupApproverGettingLive,
        PlanSetupUIState.Completed -> BackIconType.CLOSE
    }

    val loading =
        userResponse is Resource.Loading || createPolicySetupResponse is Resource.Loading

    val asyncError =
        userResponse is Resource.Error || createPolicySetupResponse is Resource.Error

    }

    enum class PlanSetupUIState {
        InviteApprovers,
        PrimaryApproverNickname,
        PrimaryApproverGettingLive,
        PrimaryApproverActivation,
        AddBackupApprover,
        BackupApproverNickname,
        BackupApproverGettingLive,
        BackupApproverActivation,
        Completed
    }

enum class BackIconType {
    Back, Exit, None
}