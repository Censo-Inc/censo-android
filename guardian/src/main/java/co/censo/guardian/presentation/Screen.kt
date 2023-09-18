package co.censo.guardian.presentation

sealed class Screen(val route: String) {
    object GuardianEntranceRoute : Screen("guardian_entrance_screen")
    companion object {
        const val GUARDIAN_DEEPLINK_ACCEPTANCE = "guardianDeepLinkAcceptance"

        const val DL_INVITATION_ID_KEY = "invitation_id_key"
    }
}
