package co.censo.censo.presentation.beneficiary_owner.beneficiary_owner_ui

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.DisabledButtonTextStyle
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.Loading

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun AddBeneficiaryLabelUI(
    label: String,
    enabled: Boolean,
    loading: Boolean,
    failedToCreateBeneficiary: String? = null,
    onLabelChanged: (String) -> Unit,
    onCreateBeneficiary: () -> Unit,
    dismissError: () -> Unit,
) {
    val verticalSpacingHeight = 24.dp

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

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
            title = R.string.label_your_beneficiary,
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        MessageText(
            modifier = Modifier.fillMaxWidth(),
            message = R.string.beneficiary_label_message,
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
                    text = stringResource(R.string.enter_a_nickname),
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

        Spacer(modifier = Modifier.height(verticalSpacingHeight * 1.5f))

        StandardButton(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            enabled = enabled,
            onClick = {
                focusRequester.freeFocus()
                keyboardController?.hide()
                onCreateBeneficiary()
            },
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 32.dp)
        ) {
            if (loading) {
                Loading(
                    strokeWidth = 3.dp,
                    size = 24.dp,
                    fullscreen = false,
                    color = Color.White
                )
            } else {
                val saveButtonTextStyle = if (enabled) ButtonTextStyle else DisabledButtonTextStyle

                Text(
                    text = stringResource(id = R.string.continue_text),
                    style = saveButtonTextStyle.copy(fontSize = 20.sp)
                )
            }
        }

        Spacer(modifier = Modifier.height(verticalSpacingHeight))
    }

    if (failedToCreateBeneficiary != null) {
        DisplayError(
            errorMessage = failedToCreateBeneficiary,
            dismissAction = dismissError,
            retryAction = null
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewEnabledLabel() {
    AddBeneficiaryLabelUI(
        label = "Yankee Hotel Foxtrot",
        enabled = true,
        loading = false,
        onLabelChanged = {},
        onCreateBeneficiary = {},
        dismissError = {}
    )
}
@Preview(showBackground = true)
@Composable
fun PreviewDisabledLabel() {
    AddBeneficiaryLabelUI(
        label = "",
        enabled = false,
        loading = true,
        onLabelChanged = {},
        onCreateBeneficiary = {},
        dismissError = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewErrorUI() {
    AddBeneficiaryLabelUI(
        label = "",
        enabled = true,
        loading = false,
        failedToCreateBeneficiary = "Unable to create beneficiary",
        onLabelChanged = {},
        onCreateBeneficiary = {},
        dismissError = {}
    )
}