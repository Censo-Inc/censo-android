package co.censo.censo.presentation.beneficiary_owner.beneficiary_owner_ui

import Base58EncodedBeneficiaryPublicKey
import Base64EncodedData
import StandardButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.data.Resource
import co.censo.shared.data.model.BeneficiaryKeyInfo
import co.censo.shared.data.model.BeneficiaryStatus
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.LargeLoading

@Composable
fun ActiveBeneficiaryUI(
    label: String,
    activatedStatus: BeneficiaryStatus.Activated,
    onProvideLegacyInformation: () -> Unit,
    onRemoveBeneficiary: () -> Unit,
    loading: Boolean,
    removeBeneficiaryError: Resource.Error<Any>?,
    dismissRemoveBeneficiaryError: () -> Unit,
) {
    val context = LocalContext.current

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    val horizontalPadding = screenWidth * 0.075f

    val topBottomPadding = screenHeight * 0.025f
    val buttonVerticalPadding = screenHeight * 0.020f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Spacer(modifier = Modifier.weight(0.15f))
        Text(
            text = stringResource(R.string.your_beneficiary_will_be_able_to_securely_access),
            fontSize = 18.sp,
            lineHeight = 22.sp,
            color = SharedColors.MainColorText,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.W500
        )
        Spacer(modifier = Modifier.weight(0.15f))
        ProspectBeneficiaryInfoBox(nickName = label, status = activatedStatus)
        Spacer(modifier = Modifier.weight(0.3f))
        Text(
            text = stringResource(R.string.your_beneficiary_may_need_additional_information),
            fontSize = 18.sp,
            lineHeight = 22.sp,
            color = SharedColors.MainColorText,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.W500
        )
        Spacer(modifier = Modifier.weight(0.1f))

        StandardButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topBottomPadding),
            contentPadding = PaddingValues(vertical = buttonVerticalPadding),
            onClick = onProvideLegacyInformation
        ) {
            Text(
                text = stringResource(R.string.provide_information),
                style = ButtonTextStyle
            )
        }
        Spacer(modifier = Modifier.height(topBottomPadding))
        StandardButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = topBottomPadding),
            contentPadding = PaddingValues(vertical = buttonVerticalPadding),
            onClick = onRemoveBeneficiary
        ) {
            Text(
                text = stringResource(R.string.remove_beneficiary),
                style = ButtonTextStyle
            )
        }
    }

    removeBeneficiaryError?.let {
        DisplayError(
            errorMessage = it.getErrorMessage(context),
            dismissAction = dismissRemoveBeneficiaryError,
            retryAction = null
        )
    }

    if (loading) {
        LargeLoading(
            color = SharedColors.DefaultLoadingColor,
            fullscreen = true,
            fullscreenBackgroundColor = Color.White,
        )
    }
}

@Preview(device = Devices.NEXUS_5, showSystemUi = true, showBackground = true)
@Composable
fun SmallPreviewActiveBeneficiaryUI() {
    ActiveBeneficiaryUI(
        label = "Ben Eficiary",
        activatedStatus = BeneficiaryStatus.Activated(
            confirmedAt = kotlinx.datetime.Clock.System.now(),
            approverContactInfo = listOf(),
            beneficiaryKeyInfo = BeneficiaryKeyInfo(
                publicKey = Base58EncodedBeneficiaryPublicKey(""),
                keySignature = Base64EncodedData(""),
                keyTimeMillis = 0L
            )
        ),
        onProvideLegacyInformation = {},
        onRemoveBeneficiary = {},
        loading = false,
        removeBeneficiaryError = null
    ) {}
}

@Preview(device = Devices.PIXEL_4, showSystemUi = true, showBackground = true)
@Composable
fun MediumPreviewActiveBeneficiaryUI() {
    ActiveBeneficiaryUI(
        label = "Ben Eficiary",
        activatedStatus = BeneficiaryStatus.Activated(
            confirmedAt = kotlinx.datetime.Clock.System.now(),
            approverContactInfo = listOf(),
            beneficiaryKeyInfo = BeneficiaryKeyInfo(
                publicKey = Base58EncodedBeneficiaryPublicKey(""),
                keySignature = Base64EncodedData(""),
                keyTimeMillis = 0L
            )
        ),
        onProvideLegacyInformation = {},
        onRemoveBeneficiary = {},
        loading = false,
        removeBeneficiaryError = null
    ) {}
}

@Preview(device = Devices.PIXEL_4_XL, showSystemUi = true, showBackground = true)
@Composable
fun LargePreviewActiveBeneficiaryUI() {
    ActiveBeneficiaryUI(
        label = "Ben Eficiary",
        activatedStatus = BeneficiaryStatus.Activated(
            confirmedAt = kotlinx.datetime.Clock.System.now(),
            approverContactInfo = listOf(),
            beneficiaryKeyInfo = BeneficiaryKeyInfo(
                publicKey = Base58EncodedBeneficiaryPublicKey(""),
                keySignature = Base64EncodedData(""),
                keyTimeMillis = 0L
            )
        ),
        onProvideLegacyInformation = {},
        onRemoveBeneficiary = {},
        loading = false,
        removeBeneficiaryError = null
    ) {}
}