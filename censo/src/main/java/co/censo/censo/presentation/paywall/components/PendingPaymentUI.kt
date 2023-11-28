package co.censo.censo.presentation.paywall.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.presentation.components.SmallLoading

@Composable
fun PendingPaymentUI() {

    PaywallBaseUI(statusSpecificContent = {
        Text(
            text = "Processing your transaction",
            fontWeight = FontWeight.W600,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            color = Color.Black,
        )
        Spacer(modifier = Modifier.height(24.dp))
        SmallLoading(
            fullscreen = false,
        )
        Spacer(modifier = Modifier.height(48.dp))
    })

}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewPendingPaymentUI() {
    PendingPaymentUI()
}