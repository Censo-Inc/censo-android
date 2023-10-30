package co.censo.guardian.presentation

import co.censo.guardian.data.ApproverUIState
import co.censo.shared.util.projectLog

sealed class Screen(val route: String) {
    object GuardianEntranceRoute : Screen("guardian_entrance_screen")

    object ApproverRoutingScreen : Screen("approver_routing_screen")

    object ApproverOnboardingScreen : Screen("approver_onboarding_screen")

}
