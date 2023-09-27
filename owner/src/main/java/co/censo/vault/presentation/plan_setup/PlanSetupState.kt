package co.censo.vault.presentation.plan_setup

import co.censo.shared.data.Resource
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.OwnerState
import co.censo.vault.presentation.components.security_plan.SetupSecurityPlanScreen
import co.censo.vault.presentation.guardian_invitation.GuardianInvitationViewModel.Companion.MIN_GUARDIAN_LIMIT

data class PlanSetupState(
    val currentScreen: SetupSecurityPlanScreen = SetupSecurityPlanScreen.Initial,
    val editingGuardian: Guardian? = null,
    val thresholdSliderPosition : Float = 1.0f,
    val ownerState: OwnerState = OwnerState.Initial,
    val addedApproverNickname: String = "",
    val showAddGuardianDialog: Boolean = false,
    val showEditOrDeleteDialog: Boolean = false,
    val guardians: List<Guardian> = emptyList(),
    val userResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
) {
    val canContinueOnboarding = guardians.size >= MIN_GUARDIAN_LIMIT

    val showBackIcon =
        currentScreen == SetupSecurityPlanScreen.RequiredApprovals
                || currentScreen == SetupSecurityPlanScreen.SecureYourPlan

    val loading =
        userResponse is Resource.Loading

    val asyncError =
        userResponse is Resource.Error
}