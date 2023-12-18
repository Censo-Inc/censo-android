package co.censo.shared.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.R
import co.censo.shared.presentation.SharedColors

@Composable
fun ConfirmationDialog(
    title: String,
    message: AnnotatedString,
    onCancel: () -> Unit, onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                modifier = Modifier.padding(8.dp),
                text = title,
                color = SharedColors.MainColorText,
                textAlign = TextAlign.Center,
                fontSize = 24.sp,
                fontWeight = FontWeight.W500,
            )
        },
        text = {
            Text(
                modifier = Modifier.padding(8.dp),
                text = message,
                color = SharedColors.MainColorText,
                textAlign = TextAlign.Start,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal
            )
        }, confirmButton = {
            TextButton(
                onClick = onDelete
            ) {
                Text(
                    stringResource(R.string.confirm),
                    color = SharedColors.MainColorText,
                    fontSize = 20.sp
                )
            }
        }, dismissButton = {
            TextButton(
                onClick = onCancel
            ) {
                Text(
                    stringResource(R.string.cancel),
                    color = SharedColors.MainColorText,
                    fontSize = 20.sp
                )
            }
        }
    )
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onCancel: () -> Unit, onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            if (title.isNotEmpty()) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = title,
                    color = SharedColors.MainColorText,
                    textAlign = TextAlign.Center,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.W500,
                )
            }
        },
        text = {
            Text(
                modifier = Modifier.padding(8.dp),
                text = message,
                color = SharedColors.MainColorText,
                textAlign = TextAlign.Start,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal
            )
        }, confirmButton = {
            TextButton(
                onClick = onDelete
            ) {
                Text(
                    stringResource(R.string.confirm),
                    color = SharedColors.MainColorText,
                    fontSize = 20.sp
                )
            }
        }, dismissButton = {
            TextButton(
                onClick = onCancel
            ) {
                Text(
                    stringResource(R.string.cancel),
                    color = SharedColors.MainColorText,
                    fontSize = 20.sp
                )
            }
        }
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewConfirmationDialog() {
    ConfirmationDialog(
        title = "Title Here",
        message = "This is a decenty long message or is it. Ok Thanks",
        onCancel = {},
        onDelete = {}
    )
}

