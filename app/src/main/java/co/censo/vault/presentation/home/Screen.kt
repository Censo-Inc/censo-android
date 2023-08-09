package co.censo.vault.presentation.home

sealed class Screen(val route: String) {
    object HomeRoute : Screen("home_screen")
    object AddBIP39Route : Screen("add_bip39_screen")

}