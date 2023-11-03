package co.censo.censo.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.censo.presentation.VaultColors

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
                color = VaultColors.PrimaryColor,
                textAlign = TextAlign.Left,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                modifier = Modifier.padding(8.dp),
                text = message,
                color = VaultColors.PrimaryColor,
                textAlign = TextAlign.Left,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm
            ) {
                Text(stringResource(R.string.yes))
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.no))
            }
        }
    )
}