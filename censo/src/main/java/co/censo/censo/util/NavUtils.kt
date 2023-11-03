package co.censo.censo.util

import androidx.navigation.NavOptionsBuilder
import co.censo.censo.presentation.Screen

fun NavOptionsBuilder.popUpToTop(shouldPopTopDestination: Boolean = true) {
    popUpTo(Screen.START_DESTINATION_ID) {
        inclusive = shouldPopTopDestination
    }
}