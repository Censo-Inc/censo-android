package co.censo.vault.presentation.plan_setup

import co.censo.shared.data.Resource
import co.censo.shared.data.model.CreatePolicySetupApiResponse
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.OwnerState
import co.censo.vault.presentation.components.security_plan.SetupSecurityPlanScreen
import co.censo.vault.presentation.guardian_invitation.GuardianInvitationViewModel.Companion.MIN_GUARDIAN_LIMIT

data class PlanSetupState(
    val currentScreen: SetupSecurityPlanScreen = SetupSecurityPlanScreen.Initial,
    val editingGuardian: Guardian.SetupGuardian? = null,
    val threshold: UInt = 1u,
    val ownerState: OwnerState = OwnerState.Initial,
    val navigateToActivateApprovers: Boolean = false,
    val addedApproverNickname: String = "",
    val showAddGuardianDialog: Boolean = false,
    val showEditOrDeleteDialog: Boolean = false,
    val guardians: List<Guardian.SetupGuardian> = emptyList(),
    val userResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val createPolicySetupResponse: Resource<CreatePolicySetupApiResponse> = Resource.Uninitialized,
) {
    val canContinueOnboarding = guardians.size >= MIN_GUARDIAN_LIMIT

    val showBackIcon =
        currentScreen == SetupSecurityPlanScreen.RequiredApprovals
                || currentScreen == SetupSecurityPlanScreen.SecureYourPlan

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