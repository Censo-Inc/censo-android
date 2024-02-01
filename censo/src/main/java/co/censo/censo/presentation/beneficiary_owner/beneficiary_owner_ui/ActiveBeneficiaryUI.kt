package co.censo.censo.presentation.beneficiary_owner.beneficiary_owner_ui

import StandardButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.data.model.BeneficiaryStatus
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors

@Composable
fun ActiveBeneficiaryUI(
    label: String,
    activatedStatus: BeneficiaryStatus.Activated,
    removeBeneficiary: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Text(
            text = "Your beneficiary will be able to securely access your seed phrases in case of unforeseen circumstances.",
            fontSize = 20.sp,
            color = SharedColors.MainColorText,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.W500
        )

        ProspectBeneficiaryInfoBox(nickName = label, status = activatedStatus)

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 16.dp),
            onClick = removeBeneficiary
        ) {
            Text(
                text = "Remove beneficiary",
                style = ButtonTextStyle
            )
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewActiveBeneficiaryUI() {
    ActiveBeneficiaryUI(
        label = "Ben Eficiary",
        activatedStatus = BeneficiaryStatus.Activated(
            confirmedAt = kotlinx.datetime.Clock.System.now()
        )
    ) {

    }
}