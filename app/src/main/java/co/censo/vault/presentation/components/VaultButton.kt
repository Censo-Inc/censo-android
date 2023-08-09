package co.censo.vault.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ButtonDefaults

@Composable
fun VaultButton(
    modifier: Modifier = Modifier,
    height: Dp? = null,
    contentPadding: PaddingValues = PaddingValues(),
    enabled: Boolean = true,
    onClick: () -> Unit,
    backgroundColor: Color = Color.Black,
    disabledBackgroundColor: Color = Color.Gray,
    content: @Composable() () -> Unit
) {

    val buttonModifier = if (height != null) modifier.height(height) else modifier

    Button(
        modifier = buttonModifier,
        shape = RoundedCornerShape(4.dp),
        contentPadding = contentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            disabledContainerColor = disabledBackgroundColor,
        ),
        enabled = enabled,
        onClick = onClick
    ) {
        content()
    }
}