package co.censo.shared

fun SharedScreen.buildScreenDeepLinkUri() = "${SharedScreen.VAULT_CUSTODY_URI}${this.route}"
sealed class SharedScreen(val route: String) {
    object HomeRoute : SharedScreen("home_screen")
    object EntranceRoute : SharedScreen("entrance_screen")

    companion object {
        //Used for setting up deep linking options for composable screens
        const val VAULT_CUSTODY_URI = "data://vault/"

        const val VAULT_GUARDIAN_URI = "vault://guardian/"
    }
}