package co.censo.censo.util

import androidx.navigation.NavOptionsBuilder
import co.censo.censo.presentation.Screen

fun NavOptionsBuilder.launchSingleTopIfNavigatingToHomeScreen(destinationRoute: String) {
    if (destinationRoute == Screen.OwnerVaultScreen.route) {
        launchSingleTop = true
    }
}
