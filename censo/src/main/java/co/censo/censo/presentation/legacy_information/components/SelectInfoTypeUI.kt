package co.censo.censo.presentation.legacy_information.components

import StandardButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors

@Composable
fun SelectInfoTypeUI(
    onApproverInfo: () -> Unit,
    onSeedPhraseInfo: () -> Unit,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val verticalSpacingHeight = screenHeight * 0.025f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .padding(horizontal = 36.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Spacer(modifier = Modifier.height(verticalSpacingHeight))
        Text(
            text = stringResource(R.string.legacy_information_type_approver_message),
            fontSize = 18.sp,
            lineHeight = 22.sp,
            color = SharedColors.MainColorText,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.W500
        )
        Spacer(modifier = Modifier.height(verticalSpacingHeight))
        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onApproverInfo,
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.approver_information),
                style = ButtonTextStyle
            )
        }
        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        Spacer(modifier = Modifier.weight(0.2f))

        Text(
            text = stringResource(R.string.legacy_information_type_seed_phrase_message),
            fontSize = 18.sp,
            lineHeight = 22.sp,
            color = SharedColors.MainColorText,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.W500
        )
        Spacer(modifier = Modifier.height(verticalSpacingHeight))
        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onSeedPhraseInfo,
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.seed_phrase_information),
                style = ButtonTextStyle
            )
        }
        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        Spacer(modifier = Modifier.weight(0.8f))
    }
}

@Preview(device = Devices.PIXEL_4_XL, showSystemUi = true, showBackground = true)
@Composable
fun LargeSelectInfoTypeUI() {
    SelectInfoTypeUI(
        onApproverInfo = {},
        onSeedPhraseInfo = {}
    )
}

@Preview(device = Devices.PIXEL_4, showSystemUi = true, showBackground = true)
@Composable
fun MediumSelectInfoTypeUI() {
    SelectInfoTypeUI(
        onApproverInfo = {},
        onSeedPhraseInfo = {}
    )
}

@Preview(device = Devices.NEXUS_5, showSystemUi = true, showBackground = true)
@Composable
fun SmallSelectInfoTypeUI() {
    SelectInfoTypeUI(
        onApproverInfo = {},
        onSeedPhraseInfo = {}
    )
}