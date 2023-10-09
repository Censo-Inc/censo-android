package co.censo.vault.presentation.components.recovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import co.censo.vault.presentation.VaultColors

@Composable
fun AnotherDeviceRecoveryScreen() {

    Column(
        Modifier
            .fillMaxSize()
            .background(color = VaultColors.PrimaryColor),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Recovery Initiated On Another Device",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.W700
        )

        Spacer(modifier = Modifier.weight(1f))
    }

}

@Preview
@Composable
fun AnotherDeviceRecoveryScreenPreview() {
    AnotherDeviceRecoveryScreen()
}