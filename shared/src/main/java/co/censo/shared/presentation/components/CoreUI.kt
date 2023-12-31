import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.DisabledButtonTextStyle
import co.censo.shared.presentation.SharedColors
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

@Composable
fun StandardButton(
    modifier: Modifier = Modifier,
    color: Color = SharedColors.ButtonBackgroundBlue,
    disabledColor : Color = SharedColors.DisabledButtonBackgroundBlue,
    borderColor: Color = Color.Transparent,
    border: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(),
    enabled: Boolean = true,
    onClick: () -> Unit,
    coolDownDuration: Duration = Duration.ZERO,
    content: @Composable RowScope.() -> Unit
) {
    var lastClickTimestamp by remember { mutableStateOf(Instant.DISTANT_PAST) }

    Button(
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            contentColor = color,
            containerColor = color,
            disabledContainerColor = disabledColor,
            disabledContentColor = disabledColor
        ),
        contentPadding = contentPadding,
        border = if (border) BorderStroke(1.dp, borderColor) else null,
        shape = RoundedCornerShape(32.dp),
        enabled = enabled,
        onClick = {
            val now = Clock.System.now()
            if (lastClickTimestamp < now.minus(coolDownDuration)) {
                onClick()
                lastClickTimestamp = now
            }
        },
    ) {
        content()
    }
}

@Composable
fun TitleText(
    modifier: Modifier = Modifier,
    @StringRes title: Int,
    fontSize: TextUnit = 24.sp,
    color: Color = SharedColors.MainColorText,
    textAlign: TextAlign = TextAlign.Center,
    fontWeight: FontWeight = FontWeight.W600
) {
    TitleText(
        modifier = modifier,
        title = stringResource(id = title),
        fontSize = fontSize,
        color = color,
        textAlign = textAlign,
        fontWeight = fontWeight
    )
}

@Composable
fun TitleText(
    modifier: Modifier = Modifier,
    title: String,
    fontSize: TextUnit = 24.sp,
    color: Color = SharedColors.MainColorText,
    textAlign: TextAlign = TextAlign.Center,
    fontWeight: FontWeight = FontWeight.W600
) {
    Text(
        modifier = modifier,
        text = title,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = textAlign
    )
}

@Composable
fun MessageText(
    modifier: Modifier = Modifier,
    @StringRes message: Int,
    color: Color = SharedColors.MainColorText,
    textAlign: TextAlign = TextAlign.Center
) {
    MessageText(
        modifier = modifier,
        message = stringResource(id = message),
        color = color,
        textAlign = textAlign
    )
}

@Composable
fun MessageText(
    modifier: Modifier = Modifier,
    message: AnnotatedString,
    color: Color = SharedColors.MainColorText,
    textAlign: TextAlign = TextAlign.Center
) {
    Text(
        modifier = modifier,
        text = message,
        color = color,
        fontSize = 16.sp,
        textAlign = textAlign
    )
}

@Composable
fun MessageText(
    modifier: Modifier = Modifier,
    message: String,
    color: Color = SharedColors.MainColorText,
    textAlign: TextAlign = TextAlign.Center
) {
    Text(
        modifier = modifier,
        text = message,
        color = color,
        fontSize = 16.sp,
        textAlign = textAlign
    )
}

@Preview
@Composable
fun ButtonPreview() {
    Box(
        modifier = Modifier
            .background(Color.White)
            .padding(36.dp),
    ) {
        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            color = SharedColors.ButtonBackgroundBlue,
            onClick = { }) {
            Text(text = "Next", style = ButtonTextStyle)
        }
    }
}

@Preview
@Composable
fun DisabledButtonPreview() {
    Box(
        modifier = Modifier
            .background(Color.White)
            .padding(36.dp),
    ) {
        StandardButton(
            modifier = Modifier.fillMaxWidth(), onClick = { }, enabled = false
        ) {
            Text(text = "Finish", style = DisabledButtonTextStyle)
        }
    }
}