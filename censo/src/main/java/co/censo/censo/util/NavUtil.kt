package co.censo.censo.util

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import co.censo.censo.presentation.Screen
import co.censo.shared.util.projectLog

data class NavigationData(
    val route: String,
    val popSelfFromBackStack: Boolean
)

fun NavOptionsBuilder.popCurrentDestinationFromBackStack(navController: NavController) {
    popUpTo(navController.currentBackStackEntry?.destination?.route ?: return) {
        inclusive = true
    }
}

fun NavOptionsBuilder.launchSingleTopIfNavigatingToHomeScreen(destinationRoute: String) {
    if (destinationRoute == Screen.OwnerVaultScreen.route) {
        projectLog(message = "Navigating to OwnerVaultScreen/HomeScreen")
        launchSingleTop = true
    }
}