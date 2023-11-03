package co.censo.guardian.presentation

sealed class Screen(val route: String) {
    object ApproverRoutingScreen : Screen("approver_routing_screen")
    object ApproverOnboardingScreen : Screen("approver_onboarding_screen")
    object ApproverAccessScreen : Screen("approver_access_screen")

}
