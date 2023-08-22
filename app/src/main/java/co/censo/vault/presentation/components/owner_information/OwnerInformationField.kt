package co.censo.vault.presentation.components.owner_information

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerInformationField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholderText: String,
    error: String,
    keyboardActions: KeyboardActions,
    keyboardOptions: KeyboardOptions
) {
    val horizontalPadding = 56.dp
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding),
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        isError = error.isNotEmpty(),
        placeholder = {
            Text(text = placeholderText)
        },
        keyboardActions = keyboardActions,
        keyboardOptions = keyboardOptions
    )

    if (error.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding),
            text = "Please enter a valid email...",
            textAlign = TextAlign.Start,
            color = Color.Red,
            fontSize = 12.sp
        )
    }
}