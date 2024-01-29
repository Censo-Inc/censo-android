package co.censo.censo.presentation.login_id_reset.components

import MessageText
import StandardButton
import TitleText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.DisabledButtonTextStyle
import co.censo.shared.presentation.SharedColors

@Composable
fun PasswordInputUI(
    onPasswordInputFinished: (String) -> Unit,
) {

    val enabledButtonStyle = ButtonTextStyle.copy(fontWeight = null)
    val disabledButtonStyle = DisabledButtonTextStyle.copy(fontWeight = null)

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(key1 = Unit) {
        focusRequester.requestFocus()
    }

    val passwordState = remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        TitleText(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            title = stringResource(R.string.password_required)
        )

        Spacer(modifier = Modifier.height(24.dp))

        MessageText(
            message = stringResource(R.string.password_required_message_1),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(6.dp))

        MessageText(
            message = stringResource(R.string.password_required_message_2),
            textAlign = TextAlign.Start

        )

        Spacer(modifier = Modifier.height(24.dp))

        BasicTextField(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = SharedColors.BorderGrey,
                    shape = RoundedCornerShape(12.dp)
                )
                .focusRequester(focusRequester),
            value = passwordState.value,
            singleLine = true,
            onValueChange = {
                passwordState.value = it
            },
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = SharedColors.MainColorText
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 14.dp)
                ) {
                    innerTextField()
                }
            },
            visualTransformation = PasswordVisualTransformation(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 14.dp),
            onClick = { onPasswordInputFinished(passwordState.value) },
            enabled = passwordState.value.isNotBlank()
        ) {
            Text(
                text = stringResource(id = R.string.continue_text),
                style = if (passwordState.value.isNotBlank()) enabledButtonStyle else disabledButtonStyle

            )
        }

    }
}


@Preview
@Composable
fun PasswordInputUIPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White),
    ) {
        PasswordInputUI(
            onPasswordInputFinished = {}
        )
    }
}