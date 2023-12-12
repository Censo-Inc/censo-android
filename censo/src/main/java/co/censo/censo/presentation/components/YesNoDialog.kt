package co.censo.censo.presentation.components

import StandardButton
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.censo.presentation.VaultColors
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors

@Composable
fun YesNoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                modifier = Modifier.padding(8.dp),
                text = title,
                color = SharedColors.MainColorText,
                textAlign = TextAlign.Left,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                modifier = Modifier.padding(8.dp),
                text = message,
                color = SharedColors.MainColorText,
                textAlign = TextAlign.Left,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal
            )
        },
        confirmButton = {
            StandardButton(
                onClick = onConfirm
            ) {
                Text(
                    stringResource(R.string.yes),
                    style = ButtonTextStyle.copy(fontSize = 17.sp)
                )
            }
        },
        dismissButton = {
            StandardButton(
                onClick = onDismiss
            ) {
                Text(
                    stringResource(R.string.no),
                    style = ButtonTextStyle.copy(fontSize = 17.sp)
                )
            }
        }
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun YesNoPreview() {
    YesNoDialog(
        title = "Title Here",
        message = "Can we see what this will look like?",
        onDismiss = {},
        onConfirm = {}
    )
}