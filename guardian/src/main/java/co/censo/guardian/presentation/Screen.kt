package co.censo.guardian.presentation

sealed class Screen(val route: String) {
    object GuardianEntranceRoute : Screen("guardian_entrance_screen")
}
