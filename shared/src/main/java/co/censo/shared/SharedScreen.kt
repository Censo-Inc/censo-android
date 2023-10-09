package co.censo.shared

fun SharedScreen.buildScreenDeepLinkUri() = "${SharedScreen.GUARDIAN_ONBOARDING_URI}${this.route}"
sealed class SharedScreen(val route: String) {
    object HomeRoute : SharedScreen("home_screen")
    object EntranceRoute : SharedScreen("entrance_screen")

    companion object {
        const val GUARDIAN_ONBOARDING_URI = "guardian://guardian/"
        const val GUARDIAN_RECOVERY_URI = "guardian://recovery/"

        const val GUARDIAN_DEEPLINK_ACCEPTANCE = "guardianDeepLinkAcceptance"

        const val DL_INVITATION_ID_KEY = "invitation_id_key"
    }
}