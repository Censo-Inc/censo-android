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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.data.model.BeneficiaryStatus
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors

@Composable
fun ActiveBeneficiaryUI(
    label: String,
    activatedStatus: BeneficiaryStatus.Activated,
    removeBeneficiary: () -> Unit
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    val horizontalPadding = screenWidth * 0.075f

    val topBottomPadding = screenHeight * 0.075f
    val buttonVerticalPadding = screenHeight * 0.020f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            modifier = Modifier.padding(top = topBottomPadding),
            text = stringResource(R.string.your_beneficiary_will_be_able_to_securely_access),
            fontSize = 18.sp,
            color = SharedColors.MainColorText,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.W500
        )

        ProspectBeneficiaryInfoBox(nickName = label, status = activatedStatus)

        StandardButton(
            modifier = Modifier.fillMaxWidth().padding(bottom = topBottomPadding),
            contentPadding = PaddingValues(vertical = buttonVerticalPadding),
            onClick = removeBeneficiary
        ) {
            Text(
                text = stringResource(R.string.remove_beneficiary),
                style = ButtonTextStyle
            )
        }
    }
}

@Preview(device = Devices.NEXUS_5, showSystemUi = true, showBackground = true)
@Composable
fun SmallPreviewActiveBeneficiaryUI() {
    ActiveBeneficiaryUI(
        label = "Ben Eficiary",
        activatedStatus = BeneficiaryStatus.Activated(
            confirmedAt = kotlinx.datetime.Clock.System.now()
        )
    ) {

    }
}

@Preview(device = Devices.PIXEL_4, showSystemUi = true, showBackground = true)
@Composable
fun MediumPreviewActiveBeneficiaryUI() {
    ActiveBeneficiaryUI(
        label = "Ben Eficiary",
        activatedStatus = BeneficiaryStatus.Activated(
            confirmedAt = kotlinx.datetime.Clock.System.now()
        )
    ) {

    }
}

@Preview(device = Devices.PIXEL_4_XL, showSystemUi = true, showBackground = true)
@Composable
fun LargePreviewActiveBeneficiaryUI() {
    ActiveBeneficiaryUI(
        label = "Ben Eficiary",
        activatedStatus = BeneficiaryStatus.Activated(
            confirmedAt = kotlinx.datetime.Clock.System.now()
        )
    ) {

    }
}