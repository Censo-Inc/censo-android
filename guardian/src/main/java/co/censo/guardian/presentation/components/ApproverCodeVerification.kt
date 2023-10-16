package co.censo.guardian.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import co.censo.shared.presentation.SharedColors.ErrorRed
import co.censo.shared.presentation.components.CodeEntry

@Composable
fun ApproverCodeVerification(
    isLoading: Boolean,
    isWaitingForVerification: Boolean,
    isVerificationRejected: Boolean,
    validCodeLength: Int,
    value: String,
    label: String,
    onValueChanged: (String) -> Unit
) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = "Become an approver",
            color = GuardianColors.PrimaryColor,
            textAlign = TextAlign.Center,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        if (!isWaitingForVerification) {
            Spacer(modifier = Modifier.height(30.dp))

            Text(
                modifier = Modifier.padding(horizontal = 40.dp),
                text = label,
                color = GuardianColors.PrimaryColor,
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(30.dp))

            //Code entry boxes
            CodeEntry(
                validCodeLength = validCodeLength,
                isLoading = isLoading,
                value = value,
                onValueChange = onValueChanged,
                primaryColor = GuardianColors.PrimaryColor
            )
        } else {
            Spacer(modifier = Modifier.height(36.dp))

            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.waiting_for_owner_verify_code),
                color = GuardianColors.PrimaryColor,
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        }

        if (isVerificationRejected) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "The code you entered in not correct. Please try again.",
                textAlign = TextAlign.Center,
                color = ErrorRed,
                modifier = Modifier.padding(horizontal = 24.dp)

            )
        }
}

@Preview
@Composable
fun ApproverCodeVerificationPreview() {
    Box(
        modifier = Modifier.background(Color.White)
    ) {
        ApproverCodeVerification(
            isLoading = false,
            isWaitingForVerification = false,
            isVerificationRejected = false,
            validCodeLength = 6,
            value = "12345",
            label = "Enter your code",
            onValueChanged = {}
        )
    }

}