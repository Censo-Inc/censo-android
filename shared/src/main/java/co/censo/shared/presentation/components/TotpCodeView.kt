package co.censo.shared.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.data.cryptography.TotpGenerator.CODE_LENGTH
import co.censo.shared.presentation.SharedColors

@Composable
fun TotpCodeView(
    code: String,
    secondsLeft: Int,
    primaryColor: Color
) {
    val formattedCode = if (code.length == CODE_LENGTH) {
        "${code.slice(0 until CODE_LENGTH / 2)}  ${code.slice(CODE_LENGTH / 2 until CODE_LENGTH)}"
    } else {
        code
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formattedCode,
            color = primaryColor,
            fontWeight = FontWeight.W600,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.width(12.dp))

        val color = when (secondsLeft) {
            in 1..20 -> SharedColors.ErrorRed
            in 21..40 -> SharedColors.WarningYellow
            else -> SharedColors.SuccessGreen
        }

        CircularProgressBar(
            number = secondsLeft,
            color = color
        )
    }
}

@Composable
fun CircularProgressBar(
    number: Int,
    fontSize: TextUnit = 16.sp,
    circlePadding: Dp = 8.dp,
    color: Color = SharedColors.SuccessGreen,
    strokeWidth: Dp = 4.dp,
) {

    val percentage = number.toFloat() / 60f

    Box(
        modifier = Modifier
            .drawBehind {
                drawArc(
                    color = color,
                    -90f,
                    360 * percentage,
                    useCenter = false,
                    style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round)
                )
            }
            .drawBehind {
                drawArc(
                    color = color.copy(alpha = 0.25f),
                    -90f,
                    360f,
                    useCenter = false,
                    style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round)
                )
            }
            .padding(circlePadding),
    ) {
        Text(
            textAlign = TextAlign.Center,
            text = if (number < 10) "0$number" else number.toString(),
            color = color,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = false)
@Composable
fun TotpCodePreview() {
    Box(modifier = Modifier.padding(24.dp)) {

        TotpCodeView(
            code = "123456",
            secondsLeft = 24,
            primaryColor = SharedColors.SuccessGreen
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProgressBarPreview() {
    Box(modifier = Modifier.padding(24.dp)) {
        CircularProgressBar(3)
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
