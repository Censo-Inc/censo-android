package co.censo.vault.presentation.components.recovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import co.censo.shared.presentation.SharedColors.ErrorRed
import co.censo.shared.presentation.components.CodeEntry
import co.censo.vault.R
import co.censo.vault.presentation.VaultColors
import androidx.compose.material3.Surface

@Composable
fun RecoveryApprovalCodeVerificationModal(
    approverLabel: String,
    isLoading: Boolean,
    isWaitingForVerification: Boolean,
    isVerificationRejected: Boolean,
    validCodeLength: Int,
    value: String,
    onValueChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(color = VaultColors.PrimaryColor),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            if (!isWaitingForVerification) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = "${stringResource(R.string.enter_code_you_got_from)} $approverLabel",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(30.dp))

                //Code entry boxes
                CodeEntry(
                    validCodeLength = validCodeLength,
                    isLoading = isLoading,
                    value = value,
                    onValueChange = onValueChanged,
                    primaryColor = Color.White
                )

                Spacer(modifier = Modifier.height(36.dp))
            }

            if (isWaitingForVerification) {
                Text(
                    text = stringResource(R.string.waiting_for_approver_to_verify_the_code),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = Color.White
                )
            }

            if (isVerificationRejected) {
                Text(
                    text = stringResource(R.string.the_code_you_entered_in_not_correct_please_try_again),
                    textAlign = TextAlign.Center,
                    color = ErrorRed,
                    modifier = Modifier.padding(horizontal = 24.dp)

                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.dismiss),
                    color = Color.White
                )
            }
        }
    }
}

@Preview
@Composable
fun RecoveryApprovalCodeVerificationScreenPreview() {
    RecoveryApprovalCodeVerificationModal(
        approverLabel = "John",
        isLoading = false,
        isWaitingForVerification = false,
        isVerificationRejected = false,
        validCodeLength = 6,
        value = "1234",
        onValueChanged = {},
        onDismiss = {}
    )
}