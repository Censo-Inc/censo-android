package co.censo.vault.util

import androidx.navigation.NavOptionsBuilder
import co.censo.vault.presentation.home.Screen

fun NavOptionsBuilder.popUpToTop(shouldPopTopDestination: Boolean = true) {
    popUpTo(Screen.START_DESTINATION_ID) {
        inclusive = shouldPopTopDestination
    }
}