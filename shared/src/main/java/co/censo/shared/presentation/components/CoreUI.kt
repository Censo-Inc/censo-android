import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.R
import co.censo.shared.presentation.SharedColors

@Composable
fun StandardButton(
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    disabledColor : Color = SharedColors.DisabledGrey,
    borderColor: Color = Color.Transparent,
    border: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(),
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
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
        onClick = onClick
    ) {
        content()
    }
}

@Composable
fun LearnMore(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.info),
            contentDescription = null,
            tint = Color.Black
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.learn_more),
            color = Color.Black
        )
    }
}

@Composable
fun TitleText(
    modifier: Modifier = Modifier,
    @StringRes title: Int,
    textAlign: TextAlign = TextAlign.Center
) {
    Text(
        modifier = modifier,
        text = stringResource(id = title),
        color = Color.Black,
        fontSize = 24.sp,
        fontWeight = FontWeight.W600,
        textAlign = textAlign
    )
}

@Composable
fun SubTitleText(
    modifier: Modifier = Modifier,
    @StringRes subtitle: Int,
    textAlign: TextAlign = TextAlign.Center
) {
    Text(
        modifier = modifier,
        text = stringResource(id = subtitle),
        color = Color.Black,
        fontSize = 18.sp,
        fontWeight = FontWeight.W600,
        textAlign = textAlign
    )
}

@Composable
fun MessageText(
    modifier: Modifier = Modifier,
    @StringRes message: Int,
    textAlign: TextAlign = TextAlign.Center
) {
    Text(
        modifier = modifier,
        text = stringResource(id = message),
        color = Color.Black,
        fontSize = 16.sp,
        textAlign = textAlign
    )
}

@Preview
@Composable
fun ButtonPreview() {
    StandardButton(color = Color.Black, onClick = { }) {
        Text(text = "Hello", color = Color.White)
    }
}


@Preview
@Composable
fun LearnMorePreview() {
    LearnMore {

    }
}
