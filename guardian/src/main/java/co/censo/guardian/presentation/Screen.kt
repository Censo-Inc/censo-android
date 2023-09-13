package co.censo.guardian.presentation

sealed class Screen(val route: String) {
    object GuardianEntranceRoute : Screen("guardian_entrance_screen")
    object HomeRoute : Screen("home_screen")

    companion object {
        const val GUARDIAN_URI = "guardian://guardian/"

        const val GUARDIAN_DEEPLINK_ACCEPTANCE = "guardianDeepLinkAcceptance"

        const val DL_DEVICE_PUBLIC_KEY_KEY = "device_public_key"
        const val DL_INTERMEDIATE_KEY_KEY = "intermediate_key"
        const val DL_PARTICIPANT_ID_KEY = "participant_id"
    }
}
