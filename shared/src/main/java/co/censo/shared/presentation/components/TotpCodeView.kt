package co.censo.shared.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.data.cryptography.TotpGenerator.CODE_LENGTH
import co.censo.shared.presentation.SharedColors

@Composable
fun TotpCodeView(
    code: String,
    percentageLeft: Float,
) {
    val formattedCode = if (code.length == CODE_LENGTH) {
        "${code.slice(0 until CODE_LENGTH / 2)}-${code.slice(CODE_LENGTH / 2 until CODE_LENGTH)}"
    } else {
        code
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formattedCode,
            color = SharedColors.PrimaryColor,
            fontWeight = FontWeight.W600,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = SharedColors.TimeLeftGray,
                    shape = CircleShape
                )
                .background(
                    color = Color.White,
                    shape = TimeLeftShape(percentageLeft)
                )
        )
    }
}

class TimeLeftShape(
    percentRemaining: Float,
) : Shape {

    private val angle = percentRemaining * 360f
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            var angle = 360f - angle
            if (angle <= 0f) {
                angle = 0.1f
            }
            moveTo(size.width / 2f, size.height / 2f)
            arcTo(Rect(0f, 0f, size.width, size.height), 270f, angle, forceMoveTo = false)
            close()
        }
        return Outline.Generic(path)
    }
}
