package co.censo.approver.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import co.censo.approver.presentation.ApproverColors


@Composable
fun ApproverTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = lightColorScheme(
        primary = ApproverColors.PrimaryColor,
        secondary = ApproverColors.AccentColor,
        tertiary = Color.White
    )

    val view = LocalView.current

    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = ApproverColors.PrimaryColor.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}