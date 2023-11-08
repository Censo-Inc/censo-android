package co.censo.shared.util

import androidx.navigation.NavOptionsBuilder

const val START_DESTINATION = 0
fun NavOptionsBuilder.popUpToTop(shouldPopTopDestination: Boolean = true) {
    popUpTo(START_DESTINATION) {
        inclusive = shouldPopTopDestination
    }
}