package co.censo.censo.presentation.beneficiary_owner.beneficiary_owner_ui

import Base64EncodedData
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.censo.presentation.plan_setup.components.ApproverActivatedUIData
import co.censo.shared.data.model.BeneficiaryInvitationId
import co.censo.shared.data.model.BeneficiaryStatus
import co.censo.shared.presentation.SharedColors
import kotlinx.datetime.Clock

@Composable
fun ProspectBeneficiaryInfoBox(
    nickName: String,
    status: BeneficiaryStatus?,
) {

    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                shape = RoundedCornerShape(12.dp), color = Color.Transparent
            )
            .border(
                width = 1.dp,
                color = SharedColors.MainBorderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        val labelTextSize = 14.sp

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = nickName,
                color = SharedColors.MainColorText,
                fontSize = 24.sp
            )

            activatedUIData(status, context).apply {
                Text(
                    text = text,
                    color = color,
                    fontSize = labelTextSize
                )
            }
        }
    }
}

fun activatedUIData(beneficiaryStatus: BeneficiaryStatus?, context: Context) =
    when (beneficiaryStatus) {
        is BeneficiaryStatus.Initial, null ->
            ApproverActivatedUIData(
                text = context.getString(R.string.not_yet_active),
                color = SharedColors.GreyText
            )

        is BeneficiaryStatus.Accepted ->
            ApproverActivatedUIData(
                text = context.getString(R.string.opened_link_in_app),
                color = SharedColors.GreyText
            )

        is BeneficiaryStatus.VerificationSubmitted ->
            ApproverActivatedUIData(
                text = context.getString(R.string.verifying),
                color = SharedColors.ErrorRed
            )

        is BeneficiaryStatus.Activated ->
            ApproverActivatedUIData(
                text = context.getString(R.string.active),
                color = SharedColors.SuccessGreen
            )
    }

@Preview
@Composable
fun ProspectApproverInfoBoxLongNamePreview() {
    Box(
        Modifier
            .background(Color.White)
            .padding(24.dp)
    ) {
        ProspectBeneficiaryInfoBox(
            nickName = "Super Long Name To Check The Edit Icon Does Not Shrink",
            status = BeneficiaryStatus.Initial(
                invitationId = BeneficiaryInvitationId(""),
                deviceEncryptedTotpSecret = Base64EncodedData("")
            ),
        )
    }
}

@Preview
@Composable
fun ProspectApproverInfoBoxRegularPreview() {
    Box(
        Modifier
            .background(Color.White)
            .padding(24.dp)
    ) {
        ProspectBeneficiaryInfoBox(
            nickName = "Goji",
            status = BeneficiaryStatus.Activated(
                confirmedAt = Clock.System.now()
            ),
        )
    }
}