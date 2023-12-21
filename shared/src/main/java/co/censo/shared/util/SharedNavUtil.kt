package co.censo.shared.util

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

data class NavigationData(
    val route: String,
    val popSelfFromBackStack: Boolean
)

const val START_DESTINATION = 0

fun NavOptionsBuilder.popCurrentDestinationFromBackStack(navController: NavController) {
    popUpTo(navController.currentBackStackEntry?.destination?.route ?: return) {
        inclusive = true
    }
}

fun NavOptionsBuilder.popUpToTop(shouldPopTopDestination: Boolean = true) {
    popUpTo(START_DESTINATION) {
        inclusive = shouldPopTopDestination
    }
}