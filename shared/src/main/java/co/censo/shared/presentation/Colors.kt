package co.censo.shared.presentation

import androidx.compose.ui.graphics.Color
object SharedColors {
    //Old design colors
    val GreyText = Color(0xFF666666)
    val DividerGray = Color(0xFFdfdfdf)
    val SuccessGreen = Color(0xFF00D890)
    val ErrorRed = Color(0xFFBC1313)
    val WarningYellow = Color(0xFFFFBF00)
    val BorderGrey = Color(0xFFBDBDBD)
    val IconGrey = Color(0xFFBDBDBD)
    val DisabledGrey = Color(0xFFEDEDED)
    val DisabledFontGrey = Color(0xFFD3D3D3)
    val PlaceholderTextGrey = Color(0xFF999999)
    val BackgroundGrey = Color(0xFFF2F2F2)

    //New design colors
    private val DarkBlue = Color(0xFF052F69)
    private val SkyBlue = Color(0xFF47F7F7)
    private val LightBlue = Color(0xFFD3FDFD)
    private val GreyBackground = Color(0xFFEBF5F6)

    val ApproverAppIcon = SkyBlue

    val ButtonBackgroundBlue = DarkBlue
    val DisabledButtonBackgroundBlue = ButtonBackgroundBlue.copy(alpha = 0.25f)
    val ButtonTextBlue = SkyBlue

    val BottomNavBarIconColor = DarkBlue
    val BottomNavBarIndicatorColor = LightBlue
    val BottomNavBarTextColor = DarkBlue

    val MainColorText = DarkBlue
    val WordBoxIconTint = DarkBlue
    val WordBoxBorder = DarkBlue

    val LoginIconColor = DarkBlue
    val ApproverStepIconColor = DarkBlue

    val MainBorderColor = DarkBlue
    val MainIconColor = DarkBlue
    val DefaultLoadingColor = DarkBlue
    val ButtonLoadingColor = SkyBlue

    val FacetecPrimaryColor = DarkBlue
    val FacetecSecondaryColor = SkyBlue

    val WordBoxTextColor = DarkBlue
    val WordBoxBackground = Color.White

    val LockScreenBackground = GreyBackground
    val MaintenanceBackground = Color.White
    val LightColorLine = SkyBlue
    val DarkColorLine = DarkBlue
    val ButtonIconColor = SkyBlue
    val BrightDividerColor = SkyBlue
}