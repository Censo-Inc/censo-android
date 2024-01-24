package co.censo.censo.presentation.enter_phrase.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.presentation.SharedColors
import co.censo.censo.R
import co.censo.censo.presentation.enter_phrase.EnterPhraseState.Companion.PHRASE_LABEL_MAX_LENGTH
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.DisabledButtonTextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPhraseLabelUI(
    label: String,
    enabled: Boolean,
    labelIsTooLong: Boolean = false,
    onLabelChanged: (String) -> Unit,
    onSavePhrase: () -> Unit,
    isRename: Boolean = false
) {

    val verticalSpacingHeight = 24.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = if (isRename) R.string.rename_your_seed_phrase else R.string.label_your_seed_phrase,
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        MessageText(
            modifier = Modifier.fillMaxWidth(),
            message = if (isRename) R.string.rename_seed_phrase_label_message else R.string.seed_phrase_label_message,
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        val textFieldStyle = TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.W500,
            color = SharedColors.MainColorText,
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = label,
            onValueChange = onLabelChanged,
            maxLines = 4,
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

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = if (labelIsTooLong) stringResource(R.string.input_string_is_too_long, PHRASE_LABEL_MAX_LENGTH) else " ",
            textAlign = TextAlign.Center,
            color = SharedColors.ErrorRed
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight / 2))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            onClick = onSavePhrase,
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 32.dp)
        ) {
            val saveButtonTextStyle = if (enabled) ButtonTextStyle else DisabledButtonTextStyle

            Text(
                text = stringResource(id = if (isRename) R.string.rename_seed_phrase else R.string.save_seed_phrase),
                style = saveButtonTextStyle.copy(fontSize = 20.sp)
            )
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
fun RenameSeedPhrase() {
    AddPhraseLabelUI(
        label = "Yankee Hotel Foxtrot",
        enabled = true,
        onLabelChanged = {},
        onSavePhrase = {},
        isRename = true,
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

@Preview(showBackground = true)
@Composable
fun PreviewLabelTooLong() {
    AddPhraseLabelUI(
        label = "",
        enabled = false,
        labelIsTooLong = true,
        onLabelChanged = {},
        onSavePhrase = {},
    )
}
