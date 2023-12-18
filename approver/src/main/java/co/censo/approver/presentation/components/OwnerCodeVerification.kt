package co.censo.approver.presentation.components

import MessageText
import TitleText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.approver.R
import co.censo.approver.presentation.ApproverColors
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.components.LargeTotpCodeView

@Composable
fun OwnerCodeVerification(
    totpCode: String?,
    secondsLeft: Int?,
    errorEnabled: Boolean
) {

    TitleText(
        modifier = Modifier.padding(horizontal = 24.dp),
        title = stringResource(R.string.read_code)
    )

    Spacer(modifier = Modifier.height(24.dp))

    MessageText(
        modifier = Modifier.padding(horizontal = 24.dp),
        message = stringResource(R.string.read_code_message)
    )

    if (totpCode == null || secondsLeft == null) {
        Text(
            text = stringResource(R.string.loading),
            textAlign = TextAlign.Center,
            fontSize = 18.sp
        )
    } else {
        Spacer(modifier = Modifier.height(30.dp))

        LargeTotpCodeView(
            totpCode,
            secondsLeft,
            SharedColors.MainColorText
        )
    }

    if (errorEnabled) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.code_entered_incorrectly),
            color = SharedColors.ErrorRed,
            fontSize = 16.sp
        )
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun OwnerCodeVerificationPreview() {

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.3f))
        OwnerCodeVerification(
            totpCode = "123456",
            secondsLeft = 35,
            errorEnabled = false
        )

        Spacer(modifier = Modifier.weight(0.7f))

    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun OwnerCodeErrorVerificationPreview() {

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.3f))
        OwnerCodeVerification(
            totpCode = "123456",
            secondsLeft = 35,
            errorEnabled = true
        )

        Spacer(modifier = Modifier.weight(0.7f))

    }
}