package co.censo.vault.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import co.censo.shared.presentation.Colors.AccentBlue
import co.censo.shared.presentation.Colors.PrimaryBlue

@Composable
fun VaultTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = lightColorScheme(
        primary = PrimaryBlue,
        secondary = AccentBlue,
        tertiary = Color.White
    )

    val view = LocalView.current

    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = PrimaryBlue.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}