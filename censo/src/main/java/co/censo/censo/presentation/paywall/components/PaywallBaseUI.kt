package co.censo.censo.presentation.paywall.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.presentation.paywall.SubscriptionOffer
import co.censo.shared.R as SharedR
import co.censo.censo.R as CensoR
import co.censo.shared.presentation.SharedColors
import co.censo.shared.util.LinksUtil
import java.time.Period

@Composable
fun PaywallBaseUI(
    userPriceText: String,
    onRestorePurchase: () -> Unit,
    statusSpecificContent: @Composable () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .padding(horizontal = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.2f))
        Text(
            text = stringResource(CensoR.string.secure_all_your_seed_phrases_for_only_1_99_month, userPriceText),
            fontWeight = FontWeight.W500,
            fontSize = 26.sp,
            lineHeight = 32.sp,
            textAlign = TextAlign.Center,
            color = SharedColors.MainColorText,
        )
        Spacer(modifier = Modifier.weight(0.2f))

        Image(
            modifier = Modifier.size(120.dp),
            painter = painterResource(id = co.censo.censo.R.drawable.censo_logo_dark_blue_stacked),
            contentDescription = null
        )

        Spacer(modifier = Modifier.weight(0.1f))

        statusSpecificContent()

        Spacer(modifier = Modifier.weight(0.05f))

        Row(
            modifier = Modifier
                .padding(
                    start = 32.dp,
                    end = 32.dp,
                )
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(SharedR.string.terms),
                modifier = Modifier.clickable { uriHandler.openUri(LinksUtil.TERMS_URL) },
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = SharedColors.MainColorText
            )
            Spacer(
                modifier = Modifier
                    .width(2.dp)
                    .height(14.dp)
                    .background(color = SharedColors.DarkColorLine)
            )
            Text(
                text = stringResource(SharedR.string.privacy),
                modifier = Modifier.clickable { uriHandler.openUri(LinksUtil.PRIVACY_URL) },
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = SharedColors.MainColorText
            )
            Spacer(
                modifier = Modifier
                    .width(2.dp)
                    .height(14.dp)
                    .background(color = SharedColors.DarkColorLine)
            )
            Text(
                text = stringResource(co.censo.censo.R.string.restore_purchases),
                modifier = Modifier.clickable { onRestorePurchase() },
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = SharedColors.MainColorText
            )
        }
        Spacer(modifier = Modifier.weight(0.08f))
    }
}

fun SubscriptionOffer.priceAndPeriodToUserText(context: Context): String {
    val formattedBillingPeriod = Period.parse(billingPeriodISO8601).let {
        when {
            it.days == 1 -> context.getString(co.censo.censo.R.string.day)
            it.months < 1 -> "${it.days} ${context.getString(co.censo.censo.R.string.days)}"
            it.months == 1 -> context.getString(co.censo.censo.R.string.month)
            it.years < 1 -> "${it.months} ${context.getString(co.censo.censo.R.string.months)}"
            else -> context.getString(co.censo.censo.R.string.year)
        }
    }

    return "$formattedPrice / $formattedBillingPeriod"
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewPaywallBaseUI() {
    PaywallBaseUI(
        userPriceText = "3.99 / month",
        onRestorePurchase = {},
    ) {
        Text(text = "GENERIC CONTENT HERE")
    }
}
