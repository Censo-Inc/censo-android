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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(screenHeight * 0.10f))
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
                    text = "You must add approvers before you can add a beneficiary.",
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
                    text = "Add beneficiary",
                    style = ButtonTextStyle,
                    color = if (addBeneficiaryEnabled) SharedColors.ButtonTextBlue else Color.White
                )
            }
            Spacer(modifier = Modifier.height(screenHeight * 0.0125f))
        }

    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewNoBeneficaryEnabledScreen() {
    NoBeneficiaryTabScreen(addBeneficiaryEnabled = true) {

    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewNoBeneficaryDisabledScreen() {
    NoBeneficiaryTabScreen(addBeneficiaryEnabled = false) {

    }
}