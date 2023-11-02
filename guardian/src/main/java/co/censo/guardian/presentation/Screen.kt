package co.censo.guardian.presentation

import co.censo.guardian.data.ApproverUIState
import co.censo.shared.SharedScreen
import co.censo.shared.util.projectLog

sealed class Screen(val route: String) {
    object ApproverRoutingScreen : Screen("approver_routing_screen")
    object ApproverOnboardingScreen : Screen("approver_onboarding_screen")
    object ApproverAccessScreen : Screen("approver_access_screen")

}
