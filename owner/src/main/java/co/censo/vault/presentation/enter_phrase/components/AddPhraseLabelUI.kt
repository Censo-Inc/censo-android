package co.censo.vault.presentation.enter_phrase.components

import LearnMore
import MessageText
import StandardButton
import TitleText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.presentation.SharedColors
import co.censo.vault.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPhraseLabelUI(
    label: String,
    enabled: Boolean,
    onLabelChanged: (String) -> Unit,
    onSavePhrase: () -> Unit
) {

    val verticalSpacingHeight = 24.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = R.string.add_a_label,
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        MessageText(
            modifier = Modifier.fillMaxWidth(),
            message = R.string.seed_phrase_label_message,
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        val textFieldStyle = TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.W500,
            color = Color.Black,
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = label,
            onValueChange = onLabelChanged,
            shape = CircleShape,
            placeholder = {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.enter_a_label),
                    fontSize = 24.sp,
                    fontWeight = textFieldStyle.fontWeight,
                    textAlign = TextAlign.Center,
                    color = SharedColors.PlaceholderTextGrey,
                )
            },
            textStyle = textFieldStyle,
            enabled = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = SharedColors.BorderGrey,
                unfocusedBorderColor = SharedColors.BorderGrey
            )
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Black,
            enabled = enabled,
            onClick = onSavePhrase,
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 32.dp)
        ) {
            Text(
                text = "Save",
                color = if (enabled) Color.White else SharedColors.DisabledFontGrey,
                fontSize = 24.sp
            )
        }

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        LearnMore {

        }

        Spacer(modifier = Modifier.height(verticalSpacingHeight))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewEnabledLabel() {
    AddPhraseLabelUI(
        label = "Yankee Hotel Foxtrot",
        enabled = true,
        onLabelChanged = {},
        onSavePhrase = {},
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewDisabledLabel() {
    AddPhraseLabelUI(
        label = "",
        enabled = false,
        onLabelChanged = {},
        onSavePhrase = {},
    )
}