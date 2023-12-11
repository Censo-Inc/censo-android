package co.censo.shared.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

val ButtonTextStyle = TextStyle(
    fontSize = 20.sp,
    fontWeight = FontWeight(600),
    color = SharedColors.ButtonTextBlue,
    textAlign = TextAlign.Center,
)

val DisabledButtonTextStyle = ButtonTextStyle.copy(color = Color.White)