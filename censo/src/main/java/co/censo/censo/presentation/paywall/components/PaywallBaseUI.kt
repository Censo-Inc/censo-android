package co.censo.censo.presentation.paywall.components

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.R
import co.censo.shared.presentation.SharedColors
import co.censo.shared.util.LinksUtil

@Composable
fun PaywallBaseUI(
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
        Spacer(modifier = Modifier.weight(0.1f))
        Text(
            text = stringResource(co.censo.censo.R.string.it_s_time_for_a_better_way),
            fontWeight = FontWeight.W600,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            color = SharedColors.MainColorText,
        )
        Spacer(modifier = Modifier.weight(0.1f))
        Text(
            text = stringResource(co.censo.censo.R.string.it_s_time_for_a_seed_phrase_manager),
            fontWeight = FontWeight.W600,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            textAlign = TextAlign.Center,
            color = SharedColors.MainColorText
        )

        Spacer(modifier = Modifier.weight(0.3f))

        Image(
            painter = painterResource(id = co.censo.censo.R.drawable.censo_login_logo),
            contentDescription = null
        )

        Spacer(modifier = Modifier.weight(0.3f))

        statusSpecificContent()

        Row(
            modifier = Modifier
                .padding(
                    start = 32.dp,
                    end = 32.dp,
                    top = 32.dp,
                    bottom = 32.dp
                )
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Text(
                text = stringResource(R.string.terms),
                modifier = Modifier.clickable { uriHandler.openUri(LinksUtil.TERMS_URL) },
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = SharedColors.MainColorText
            )
            Text(
                text = stringResource(R.string.privacy),
                modifier = Modifier.clickable { uriHandler.openUri(LinksUtil.PRIVACY_URL) },
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = SharedColors.MainColorText
            )
        }
    }
}
