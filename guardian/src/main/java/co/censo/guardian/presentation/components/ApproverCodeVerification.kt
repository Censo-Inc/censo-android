package co.censo.guardian.presentation.components

import MessageText
import TitleText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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
import co.censo.guardian.R
import co.censo.guardian.presentation.GuardianColors
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.SharedColors.ErrorRed
import co.censo.shared.presentation.components.CodeEntry

@Composable
fun ApproverCodeVerification(
    isLoading: Boolean,
    codeVerificationStatus: CodeVerificationStatus,
    validCodeLength: Int,
    value: String,
    onValueChanged: (String) -> Unit
) {

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Spacer(modifier = Modifier.height(30.dp))

        val title =
            if (codeVerificationStatus == CodeVerificationStatus.Waiting) stringResource(R.string.code_entered)
            else stringResource(R.string.enter_the_code)

        TitleText(title = title)

        Spacer(modifier = Modifier.height(30.dp))

        val messageText = when (codeVerificationStatus) {
            CodeVerificationStatus.Waiting -> stringResource(R.string.waiting_on_owner_to_approve_code)
            CodeVerificationStatus.Rejected -> stringResource(R.string.incorrect_code_entered_please_try_again)
            CodeVerificationStatus.Initial -> stringResource(R.string.enter_the_6_digit_code_from_the_seed_phrase_owner)
        }

        MessageText(message = messageText)

        Spacer(modifier = Modifier.height(30.dp))

        if (codeVerificationStatus != CodeVerificationStatus.Waiting) {
            //Code entry box
            CodeEntry(
                length = validCodeLength,
                enabled = !isLoading || codeVerificationStatus != CodeVerificationStatus.Waiting,
                value = value,
                onValueChange = onValueChanged,
                primaryColor = GuardianColors.PrimaryColor,
                borderColor = SharedColors.BorderGrey,
                backgroundColor = SharedColors.WordBoxBackground
            )
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

enum class CodeVerificationStatus {
    Waiting, Rejected, Initial
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ApproverCodeVerificationPreview() {
    Box(
        modifier = Modifier.background(Color.White)
    ) {
        ApproverCodeVerification(
            isLoading = false,
            codeVerificationStatus = CodeVerificationStatus.Initial,
            validCodeLength = 6,
            value = "12345",
            onValueChanged = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ApproverCodeVerificationWaitingPreview() {
    Box(
        modifier = Modifier.background(Color.White)
    ) {
        ApproverCodeVerification(
            isLoading = false,
            codeVerificationStatus = CodeVerificationStatus.Waiting,
            validCodeLength = 6,
            value = "12345",
            onValueChanged = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ApproverCodeVerificationRejectedPreview() {
    Box(
        modifier = Modifier.background(Color.White)
    ) {
        ApproverCodeVerification(
            isLoading = false,
            codeVerificationStatus = CodeVerificationStatus.Rejected,
            validCodeLength = 6,
            value = "12345",
            onValueChanged = {}
        )
    }
}