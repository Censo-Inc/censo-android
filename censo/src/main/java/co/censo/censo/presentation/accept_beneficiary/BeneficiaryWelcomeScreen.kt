package co.censo.censo.presentation.accept_beneficiary

import StandardButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors

@Composable
fun WelcomeBeneficiaryScreen(
    title: String,
    message: String,
    buttonText: String,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                title,
                fontSize = 44.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                color = SharedColors.MainColorText,
                lineHeight = 48.sp,
                modifier = Modifier
                    .padding(top = 24.dp, bottom = 24.dp)
                    .fillMaxWidth()
            )

            Text(
                text = message,
                color = SharedColors.MainColorText,
                fontSize = 20.sp,
                fontWeight = FontWeight.W500
            )
        }

        StandardButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            onClick = onContinue
        ) {
            Text(
                text = buttonText,
                style = ButtonTextStyle
            )
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewStandardWelcomeBeneficiary() {
    WelcomeBeneficiaryScreen(
        title = stringResource(R.string.welcome_to_censo),
        message = stringResource(R.string.standard_beneficiary_message),
        buttonText = stringResource(id = R.string.continue_text),
    ) {

    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewReEnterWelcomeBeneficiary() {
    WelcomeBeneficiaryScreen(
        title = stringResource(R.string.becoming_a_beneficiary),
        message = stringResource(R.string.re_enter_beneficiary_message),
        buttonText = stringResource(id = R.string.paste_from_clipboard),
    ) {

    }
}