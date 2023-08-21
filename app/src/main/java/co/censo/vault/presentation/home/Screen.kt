package co.censo.vault.presentation.home

sealed class Screen(val route: String) {
    object HomeRoute : Screen("home_screen")
    object AddBIP39Route : Screen("add_bip39_screen")
    object BIP39DetailRoute : Screen("bip_39_detail_screen") {
        const val BIP_39_NAME_ARG = "bip39_name"
    }

    fun buildScreenDeepLinkUri() = "$VAULT_CUSTODY_URI${this.route}"
    companion object {
        const val START_DESTINATION_ID = 0

        //Used for setting up deep linking options for composable screens
        const val VAULT_CUSTODY_URI = "data://vault/"

        const val GUARDIAN_DEEPLINK_ACCEPTANCE = "guardianDeepLinkAcceptance"

        const val DL_TOKEN_KEY = "token"
    }
}