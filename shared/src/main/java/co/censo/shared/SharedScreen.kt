package co.censo.shared

fun SharedScreen.buildScreenDeepLinkUri() = "${SharedScreen.GUARDIAN_ONBOARDING_URI}${this.route}"
sealed class SharedScreen(val route: String) {
    object HomeRoute : SharedScreen("home_screen")
    object EntranceRoute : SharedScreen("entrance_screen")
    object OwnerVaultScreen : SharedScreen("owner_vault_screen")
    object OwnerWelcomeScreen : SharedScreen("owner_welcome_screen")

    companion object {
        const val GUARDIAN_ONBOARDING_URI = "censo-guardian://invite/"
        const val GUARDIAN_RECOVERY_URI = "censo-guardian://recovery/"

        const val GUARDIAN_DEEPLINK_ACCEPTANCE = "guardianDeepLinkAcceptance"
        const val DL_INVITATION_ID_KEY = "invitation_id_key"

        const val GUARDIAN_DEEPLINK_RECOVERY = "guardianDeepLinkRecovery"
        const val DL_PARTICIPANT_ID_KEY = "participant_id_key"

    }
}