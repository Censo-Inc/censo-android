package co.censo.censo.presentation.beneficiary_home.ui

import StandardButton
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
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
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.components.SmallLoading

@Composable
fun BeneficiaryHomeUI(
    loading: Boolean,
    initiateTakeover: () -> Unit,
    showSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .background(color = Color.White)
            .padding(horizontal = 32.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(3.0f))

        Text(
            stringResource(R.string.welcome_back_to_censo),
            fontSize = 34.sp,
            fontWeight = FontWeight.W400,
            textAlign = TextAlign.Start,
            color = SharedColors.MainColorText,
            lineHeight = 36.sp,
            modifier = Modifier
                .padding(top = 24.dp, bottom = 24.dp)
                .fillMaxWidth()
        )
        Text(
            stringResource(R.string.beneficiary_home_message),
            fontSize = 18.sp,
            fontWeight = FontWeight.W400,
            textAlign = TextAlign.Start,
            color = SharedColors.MainColorText,
        )

        Spacer(modifier = Modifier.weight(1.5f))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp),
            onClick = initiateTakeover
        ) {
            if (loading) {
                SmallLoading(fullscreen = false, color = SharedColors.ButtonLoadingColor)
            } else {
                Text(
                    text = stringResource(R.string.initiate_takeover),
                    style = ButtonTextStyle.copy(fontSize = 20.sp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(3.0f))

        Row(
            modifier = Modifier.clickable { showSettings() },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                tint = SharedColors.MainIconColor,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.settings),
                color = SharedColors.MainColorText,
                fontSize = 24.sp,
                fontWeight = FontWeight.W500,
            )
        }

        Spacer(modifier = Modifier.weight(1.0f))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewBeneficiaryHomeScreen() {
    BeneficiaryHomeUI(loading = false, {}, {})
}