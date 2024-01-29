package co.censo.shared.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.R
import co.censo.shared.presentation.SharedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmationDialog(
    title: String,
    message: AnnotatedString,
    confirmationText: String? = null,
    onCancel: () -> Unit, onDelete: () -> Unit
) {
    var enteredConfirmationText by remember { mutableStateOf("") }
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
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = message,
                    color = SharedColors.MainColorText,
                    textAlign = TextAlign.Start,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal
                )
                if (confirmationText != null) {
                    OutlinedTextField(
                        value = enteredConfirmationText,
                        onValueChange = { enteredConfirmationText = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        enabled = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        shape = RoundedCornerShape(8.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = SharedColors.MainBorderColor,
                            unfocusedBorderColor = SharedColors.MainBorderColor
                        ),
                        textStyle = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal,
                            color = SharedColors.MainColorText
                        )
                    )
                }
            }
        }, confirmButton = {
            val enabled = confirmationText == null || confirmationText == enteredConfirmationText
            TextButton(
                onClick = onDelete,
                enabled = enabled
            ) {
                Text(
                    stringResource(R.string.confirm),
                    color = if (enabled) SharedColors.MainColorText else SharedColors.GreyText,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmationText: String? = null,
    onCancel: () -> Unit, onDelete: () -> Unit
) {
    var enteredConfirmationText by remember { mutableStateOf("") }
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
            Column {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = message,
                    color = SharedColors.MainColorText,
                    textAlign = TextAlign.Start,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal
                )
                if (confirmationText != null) {
                    OutlinedTextField(
                        value = enteredConfirmationText,
                        onValueChange = { enteredConfirmationText = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        enabled = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        shape = RoundedCornerShape(8.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = SharedColors.MainBorderColor,
                            unfocusedBorderColor = SharedColors.MainBorderColor
                        ),
                        textStyle = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal,
                            color = SharedColors.MainColorText
                        )
                    )
                }
            }
        }, confirmButton = {
            TextButton(
                onClick = onDelete,
                enabled = confirmationText == null || confirmationText == enteredConfirmationText
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
        confirmationText = "CONFIRM",
        onCancel = {},
        onDelete = {}
    )
}

