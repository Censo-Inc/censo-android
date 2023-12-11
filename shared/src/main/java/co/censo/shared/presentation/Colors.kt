package co.censo.shared.presentation

import androidx.compose.ui.graphics.Color
object SharedColors {
    //Old design colors
    val GreyText = Color(0xFF666666)
    val DividerGray = Color(0xFFdfdfdf)
    val SuccessGreen = Color(0xFF00D890)
    val ErrorRed = Color(0xFFBC1313)
    val WarningYellow = Color(0xFFFFBF00)
    val WordBoxBackground = Color(0xFFF2F2F2)
    val BorderGrey = Color(0xFFBDBDBD)
    val IconGrey = Color(0xFFBDBDBD)
    val DisabledGrey = Color(0xFFEDEDED)
    val DisabledFontGrey = Color(0xFFD3D3D3)
    val PlaceholderTextGrey = Color(0xFF999999)
    val BackgroundGrey = Color(0xFFF2F2F2)

    //New design colors
    private val DarkBlue = Color(0xFF052F69)
    private val SkyBlue = Color(0xFF47F7F7)
    private val LightBlue = Color(0xFFd7f1f2)

    val ButtonBackgroundBlue = DarkBlue
    val DisabledButtonBackgroundBlue = ButtonBackgroundBlue.copy(alpha = 0.25f)
    val ButtonTextBlue = SkyBlue

    val BottomNavBarIconColor = DarkBlue
    val BottomNavBarIndicatorColor = LightBlue
    val BottomNavBarTextColor = DarkBlue
}