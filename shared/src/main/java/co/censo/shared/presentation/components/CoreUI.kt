import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.censo.shared.R

@Composable
fun FullScreenButton(
    modifier: Modifier = Modifier,
    color: Color,
    borderColor: Color = Color.Transparent,
    border: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(),
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            contentColor = color,
            containerColor = color
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

@Preview
@Composable
fun ButtonPreview() {
    FullScreenButton(color = Color.Black, onClick = { }) {
        Text(text = "Hello", color = Color.White)
    }
}


@Preview
@Composable
fun LearnMorePreview() {
    LearnMore {

    }
}
