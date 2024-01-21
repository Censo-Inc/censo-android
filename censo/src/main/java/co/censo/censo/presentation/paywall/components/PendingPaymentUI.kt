package co.censo.censo.presentation.paywall.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.censo.presentation.paywall.SubscriptionOffer
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.components.SmallLoading

@Composable
fun PendingPaymentUI(
    offer: SubscriptionOffer,
    onCancel: (() -> Unit)?
) {
    PaywallBaseUI(
        userPriceText = offer.priceAndPeriodToUserText(LocalContext.current),
        onCancel = onCancel,
        statusSpecificContent = {
        Text(
            text = stringResource(R.string.processing_your_transaction),
            fontWeight = FontWeight.W600,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            color = SharedColors.MainColorText
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
    PendingPaymentUI(
        offer = SubscriptionOffer(
            productId = "co.censo.standard.1month",
            offerToken = "YhgOLTsGIrAD8KF",
            formattedPrice = "$1.99",
            billingPeriodISO8601 = "P1M",
            feeTrialPeriodISO8601 = "P7D",
        ),
        onCancel = null
    )
}