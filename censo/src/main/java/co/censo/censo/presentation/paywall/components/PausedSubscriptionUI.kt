package co.censo.censo.presentation.paywall.components

import StandardButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import co.censo.censo.R
import co.censo.censo.presentation.paywall.SubscriptionOffer
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors
import java.time.Period

@Composable
fun PausedSubscriptionUI(
    offer: SubscriptionOffer,
    onContinue: (SubscriptionOffer) -> Unit,
) {

    val formattedBillingPeriod = Period.parse(offer.billingPeriodISO8601).let {
        when {
            it.days == 1 -> stringResource(R.string.day)
            it.months < 1 -> "${it.days} ${stringResource(R.string.days)}"
            it.months == 1 -> stringResource(R.string.month)
            it.years < 1 -> "${it.months} ${stringResource(R.string.months)}"
            else -> stringResource(R.string.year)
        }
    }

    val priceText = "${offer.formattedPrice} / $formattedBillingPeriod"

    val trialText = offer.feeTrialPeriodISO8601
        ?.let { "${Period.parse(it).days} ${stringResource(R.string.days_free_then)} " }
        ?: ""

    PaywallBaseUI(statusSpecificContent = {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp),
            text = stringResource(R.string.your_subscription_was_paused),
            fontWeight = FontWeight.W600,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            color = SharedColors.MainColorText
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            modifier = Modifier.padding(horizontal = 12.dp),
            text = stringResource(R.string.reactivate_to_continue_cancel_anytime),
            fontWeight = FontWeight.W600,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            color = SharedColors.MainColorText
        )
        Spacer(modifier = Modifier.height(24.dp))
        StandardButton(
            onClick = { onContinue(offer) },
            contentPadding = PaddingValues(
                horizontal = 36.dp,
                vertical = 12.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.continue_text),
                    style = ButtonTextStyle.copy(fontSize = 28.sp, fontWeight = FontWeight.Medium),
                )
                Text(
                    text = "$trialText$priceText",
                    style = ButtonTextStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium),
                )
            }
        }
    })
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewPausedSubscriptionUI() {
    PausedSubscriptionUI(
        offer = SubscriptionOffer(
            productId = "co.censo.standard.1month",
            offerToken = "YhgOLTsGIrAD8KF",
            formattedPrice = "$1.99",
            billingPeriodISO8601 = "P1M",
            feeTrialPeriodISO8601 = null,
        ),
        onContinue = {},
    )
}