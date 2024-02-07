package co.censo.censo.presentation.beneficiary_owner.beneficiary_owner_ui

import StandardButton
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
fun NoBeneficiaryTabScreen(
    addBeneficiaryEnabled: Boolean,
    addBeneficiary: () -> Unit
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(screenHeight * 0.07f))
            Text(
                text = stringResource(id = R.string.beneficiary_explainer_text),
                color = SharedColors.MainColorText,
                fontWeight = FontWeight.W500,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!addBeneficiaryEnabled) {
                Text(
                    text = stringResource(R.string.you_must_add_approvers_before_you_can_add_a_beneficiary),
                    color = SharedColors.ErrorRed,
                    fontWeight = FontWeight.W500,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(screenHeight * 0.025f))
            }
            StandardButton(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 16.dp),
                onClick = addBeneficiary,
                enabled = addBeneficiaryEnabled
            ) {
                Text(
                    text = stringResource(R.string.add_beneficiary),
                    style = ButtonTextStyle,
                    color = if (addBeneficiaryEnabled) SharedColors.ButtonTextBlue else Color.White
                )
            }
            Spacer(modifier = Modifier.height(screenHeight * 0.015f))
        }

    }
}

@Preview(device = Devices.NEXUS_5, showBackground = true, showSystemUi = true)
@Composable
fun SmallPreviewNoBeneficiaryEnabledScreen() {
    NoBeneficiaryTabScreen(addBeneficiaryEnabled = true) {

    }
}

@Preview(device = Devices.PIXEL_4, showBackground = true, showSystemUi = true)
@Composable
fun MediumPreviewNoBeneficiaryEnabledScreen() {
    NoBeneficiaryTabScreen(addBeneficiaryEnabled = true) {

    }
}

@Preview(device = Devices.PIXEL_4_XL, showBackground = true, showSystemUi = true)
@Composable
fun LargePreviewNoBeneficiaryEnabledScreen() {
    NoBeneficiaryTabScreen(addBeneficiaryEnabled = true) {

    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewNoBeneficiaryDisabledScreen() {
    NoBeneficiaryTabScreen(addBeneficiaryEnabled = false) {

    }
}