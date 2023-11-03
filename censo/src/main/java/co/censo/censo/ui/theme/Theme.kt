package co.censo.censo.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import co.censo.censo.presentation.VaultColors.AccentColor
import co.censo.censo.presentation.VaultColors.PrimaryColor

@Composable
fun VaultTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = lightColorScheme(
        primary = PrimaryColor,
        secondary = AccentColor,
        tertiary = Color.White
    )

    val view = LocalView.current

    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = PrimaryColor.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}