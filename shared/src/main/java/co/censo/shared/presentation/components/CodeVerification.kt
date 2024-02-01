package co.censo.shared.presentation.components

import MessageText
import TitleText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.censo.shared.presentation.SharedColors

import co.censo.shared.R as SharedR

@Composable
fun CodeVerificationUI(
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
            if (codeVerificationStatus == CodeVerificationStatus.Waiting) stringResource(SharedR.string.code_entered)
            else stringResource(SharedR.string.enter_the_code)
        TitleText(title = title)

        Spacer(modifier = Modifier.height(30.dp))

        val messageText = when (codeVerificationStatus) {
            CodeVerificationStatus.Waiting -> stringResource(SharedR.string.waiting_on_owner_to_approve_code)
            CodeVerificationStatus.Rejected -> stringResource(SharedR.string.incorrect_code_entered_please_try_again)
            CodeVerificationStatus.Initial -> stringResource(SharedR.string.enter_the_6_digit_code_from_the_seed_phrase_owner)
        }

        if (codeVerificationStatus == CodeVerificationStatus.Waiting) {
            SmallLoading(fullscreen = false)
            Spacer(modifier = Modifier.height(12.dp))
        }

        val messageTextColor =
            if (codeVerificationStatus == CodeVerificationStatus.Waiting) SharedColors.GreyText else SharedColors.MainColorText
        MessageText(message = messageText, color = messageTextColor)

        Spacer(modifier = Modifier.height(30.dp))

        //Code entry box
        CodeEntry(
            length = validCodeLength,
            enabled = !isLoading,
            value = value,
            onValueChange = onValueChanged,
            primaryColor = SharedColors.MainColorText,
            borderColor = SharedColors.BorderGrey,
            backgroundColor = SharedColors.WordBoxBackground
        )
        Spacer(modifier = Modifier.height(36.dp))
    }
    KeepScreenOn()
}

enum class CodeVerificationStatus {
    Waiting, Rejected, Initial
}