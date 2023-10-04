import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FullScreenButton(
    modifier: Modifier = Modifier,
    color: Color,
    borderColor: Color,
    border: Boolean,
    contentPadding: PaddingValues,
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
        shape = RoundedCornerShape(4.dp),
        enabled = enabled,
        onClick = onClick
    ) {
        content()
    }
}