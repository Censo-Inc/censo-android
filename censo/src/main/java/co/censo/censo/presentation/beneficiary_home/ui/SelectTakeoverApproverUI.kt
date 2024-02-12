package co.censo.censo.presentation.beneficiary_home.ui

import Base64EncodedData
import ParticipantId
import StandardButton
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.censo.presentation.access_approval.components.SelectingApproverInfoBox
import co.censo.shared.DeepLinkURI
import co.censo.shared.data.model.BeneficiaryApproverContactInfo
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors

@Composable
fun SelectTakeoverApproverUI(
    approvers: List<BeneficiaryApproverContactInfo>,
    selectedApprover: ParticipantId,
    takeoverId: String,
    onSelectedApprover: (BeneficiaryApproverContactInfo) -> Unit,
) {
    val context = LocalContext.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    val largeHeight = screenHeight * 0.025f
    val mediumHeight = screenHeight * 0.0125f

    val link = DeepLinkURI.createTakeoverApproverDeepLink(selectedApprover.value, takeoverId)

    fun shareLink(link: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, link)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(
                R.string.takeover_initiatied_message
            ),
            color = SharedColors.MainColorText,
            fontSize = 16.sp,
        )
        Spacer(modifier = Modifier.height(largeHeight))
        for (approver in approvers) {
            Spacer(modifier = Modifier.height(mediumHeight))
            Box(
                modifier = Modifier.clickable { onSelectedApprover(approver) }
            ) {
                SelectingApproverInfoBox(
                    nickName = approver.label,
                    selected = selectedApprover == approver.participantId,
                    approved = false,
                    sidePadding = 0.dp,
                    internalPadding = PaddingValues(vertical = largeHeight, horizontal = 24.dp),
                    onSelect = {
                        onSelectedApprover(approver)
                    }
                )
            }
            Spacer(modifier = Modifier.height(mediumHeight))
        }

        Spacer(modifier = Modifier.weight(0.3f))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp),
            onClick = { shareLink(link) }
        ) {
            Row {
                Icon(
                    imageVector = Icons.Default.IosShare,
                    contentDescription = null,
                    tint = SharedColors.ButtonIconColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.share),
                    style = ButtonTextStyle.copy(fontSize = 20.sp)
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewSelectTakeoverApprover() {
    val selectedApprover = ParticipantId.generate()
    SelectTakeoverApproverUI(
        approvers = listOf(
            BeneficiaryApproverContactInfo(
                participantId = selectedApprover,
                label = "Sam",
                encryptedContactInfo = Base64EncodedData("")
            ),
            BeneficiaryApproverContactInfo(
                participantId = ParticipantId.generate(),
                label = "Jason",
                encryptedContactInfo = Base64EncodedData("")
            )
        ),
        selectedApprover = selectedApprover,
        takeoverId = "link",
        onSelectedApprover = {}
    )
}