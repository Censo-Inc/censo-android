package co.censo.censo.presentation.components

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.censo.censo.R
import co.censo.shared.presentation.ButtonTextStyle


enum class AnotherDeviceInitiatedFlow {
    Access, BiometryReset
}

@Composable
fun AnotherDeviceInitiatedFlowUI(
    flow: AnotherDeviceInitiatedFlow,
    onCancel: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = when (flow) {
                AnotherDeviceInitiatedFlow.Access -> stringResource(R.string.access_was_requested_using_another_phone)
                AnotherDeviceInitiatedFlow.BiometryReset -> stringResource(R.string.biometry_reset_was_requested_using_another_phone)
            },
            textAlign = TextAlign.Start,
        )

        Spacer(modifier = Modifier.height(12.dp))

        MessageText(
            modifier = Modifier.fillMaxWidth(),
            message = when (flow) {
                AnotherDeviceInitiatedFlow.Access -> stringResource(R.string.access_was_requested_using_another_phone_blurb)
                AnotherDeviceInitiatedFlow.BiometryReset -> stringResource(R.string.biometry_reset_was_requested_using_another_phone_blurb)
            },
            textAlign = TextAlign.Start,
        )

        Spacer(modifier = Modifier.height(24.dp))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp),
            onClick = onCancel
        ) {
            Text(
                text = when (flow) {
                    AnotherDeviceInitiatedFlow.Access -> stringResource(R.string.cancel_access)
                    AnotherDeviceInitiatedFlow.BiometryReset -> stringResource(R.string.cancel_biometry_reset)
                },
                style = ButtonTextStyle
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

    }

}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun AnotherDeviceInitiatedAccessUIPreview() {
    AnotherDeviceInitiatedFlowUI(
        flow = AnotherDeviceInitiatedFlow.Access,
        onCancel = {}
    )
}
@Preview(showSystemUi = true, showBackground = true)
@Composable
fun AnotherDeviceInitiatedBiometryResetUIPreview() {
    AnotherDeviceInitiatedFlowUI(
        flow = AnotherDeviceInitiatedFlow.BiometryReset,
        onCancel = {}
    )
}