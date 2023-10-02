package co.censo.vault.presentation.plan_setup

import co.censo.shared.data.Resource
import co.censo.shared.data.model.CreatePolicySetupApiResponse
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.SecurityPlanData
import co.censo.vault.presentation.components.security_plan.SetupSecurityPlanScreen
import co.censo.vault.presentation.activate_approvers.ActivateApproversViewModel.Companion.MIN_GUARDIAN_LIMIT

data class PlanSetupState(
    //Screen in Plan Setup Flow
    val currentScreen: SetupSecurityPlanScreen = SetupSecurityPlanScreen.Initial,

    //In Progress UI Data
    val navigateToActivateApprovers: Boolean = false,
    val addedApproverNickname: String = "",
    val editingGuardian: Guardian.SetupGuardian? = null,

    //Dialog Triggers
    val showAddGuardianDialog: Boolean = false,
    val showEditOrDeleteDialog: Boolean = false,
    val showCancelPlanSetupDialog: Boolean = false,

    //Plan Data
    val guardians: List<Guardian.SetupGuardian> = emptyList(),
    val threshold: UInt = 1u,
    val existingSecurityPlan : SecurityPlanData? = null,

    //API Calls
    val userResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val createPolicySetupResponse: Resource<CreatePolicySetupApiResponse> = Resource.Uninitialized,
) {
    val canContinueOnboarding = guardians.size >= MIN_GUARDIAN_LIMIT

    val showBackIcon: BackIconType =

        when (currentScreen) {
            SetupSecurityPlanScreen.RequiredApprovals,
            SetupSecurityPlanScreen.SecureYourPlan -> BackIconType.Back
            SetupSecurityPlanScreen.Review -> existingSecurityPlan?.let { BackIconType.Exit }
                ?: BackIconType.None
            else -> BackIconType.None
        }

    val mainButtonCount = when (currentScreen) {
        SetupSecurityPlanScreen.Initial,
        SetupSecurityPlanScreen.AddApprovers,
        SetupSecurityPlanScreen.SecureYourPlan,
        SetupSecurityPlanScreen.RequiredApprovals -> 2
        SetupSecurityPlanScreen.Review -> 1
        SetupSecurityPlanScreen.FacetecAuth -> 0
    }

    val loading =
        userResponse is Resource.Loading || createPolicySetupResponse is Resource.Loading

    val asyncError =
        userResponse is Resource.Error || createPolicySetupResponse is Resource.Error
}

enum class BackIconType {
    Back, Exit, None
}