package co.censo.censo.presentation.access_approval.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.presentation.SharedColors
import co.censo.censo.R

@Composable
fun AnotherDeviceAccessScreen(
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
            title = stringResource(R.string.access_was_requested_using_another_phone),
            textAlign = TextAlign.Start,
        )

        Spacer(modifier = Modifier.height(12.dp))

        MessageText(
            modifier = Modifier.fillMaxWidth(),
            message = stringResource(R.string.access_was_requested_using_another_phone_blurb),
            textAlign = TextAlign.Start,
        )

        Spacer(modifier = Modifier.height(24.dp))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = true,
            disabledColor = SharedColors.DisabledGrey,
            color = Color.Black,
            contentPadding = PaddingValues(vertical = 12.dp),
            onClick = onCancel
        ) {
            Text(
                fontSize = 20.sp,
                text = stringResource(R.string.cancel_access),
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

    }

}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun AnotherDeviceRecoveryScreenPreview() {
    AnotherDeviceAccessScreen(
        onCancel = {}
    )
}