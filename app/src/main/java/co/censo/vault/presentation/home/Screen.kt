package co.censo.vault.presentation.home

sealed class Screen(val route: String) {
    object HomeRoute : Screen("home_screen")
    object AddBIP39Route : Screen("add_bip39_screen")
    object BIP39DetailRoute : Screen("bip_39_detail_screen") {
        const val BIP_39_NAME_ARG = "bip39_name"
    }
    companion object {
        const val START_DESTINATION_ID = 0
    }
}