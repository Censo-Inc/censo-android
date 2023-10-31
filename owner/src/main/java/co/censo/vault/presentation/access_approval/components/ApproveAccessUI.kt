package co.censo.vault.presentation.access_approval.components

import LearnMore
import MessageText
import TitleText
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.model.Approval
import co.censo.shared.data.model.ApprovalStatus
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.components.CodeEntry
import co.censo.vault.R
import co.censo.vault.presentation.plan_setup.components.ActionButtonUIData
import co.censo.vault.presentation.plan_setup.components.ApproverStep

@Composable
fun ApproveAccessUI(
    approval: Approval,
    verificationCode: String,
    onVerificationCodeChanged: (String) -> Unit,
    storesLink: String, //This should contain both links since approver could be either
) {

    val deeplink = approval.deepLink()
    val codeEditable = approval.status in listOf(ApprovalStatus.WaitingForVerification, ApprovalStatus.Rejected)

    val context = LocalContext.current

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
            .fillMaxSize()
            .padding(horizontal = 36.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {

        Spacer(modifier = Modifier.height(24.dp))

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = R.string.request_access,
            textAlign = TextAlign.Start,
        )

        Spacer(modifier = Modifier.height(12.dp))

        MessageText(
            modifier = Modifier.fillMaxWidth(),
            message = R.string.approver_do_3_things,
            textAlign = TextAlign.Start,
        )

        Spacer(modifier = Modifier.height(24.dp))

        ApproverStep(
            imagePainter = painterResource(id = co.censo.shared.R.drawable.share_link),
            heading = stringResource(id = R.string.download_the_app_title),
            content = stringResource(id = R.string.dowloand_the_app_message),
            actionButtonUIData = ActionButtonUIData(
                onClick = { shareLink(storesLink) },
                text = stringResource(R.string.share_app_link),
            ),
        )

        Spacer(modifier = Modifier.height(12.dp))

        ApproverStep(
            imagePainter = painterResource(id = co.censo.shared.R.drawable.share_link),
            heading = stringResource(id = R.string.share_the_link_title),
            content = stringResource(id = R.string.share_the_link_message),
            actionButtonUIData = ActionButtonUIData(
                onClick = { shareLink(deeplink) },
                text = stringResource(R.string.share_unique_link)
            ),
        )

        Spacer(modifier = Modifier.height(12.dp))

        ApproverStep(
            imagePainter = painterResource(id = co.censo.shared.R.drawable.phrase_entry),
            heading = "3. Enter the code",
            content = "Enter the 6-digit code from your approver.",
        )
        if (approval.status == ApprovalStatus.Rejected) {
            Text(
                text = stringResource(R.string.the_code_you_entered_in_not_correct),
                textAlign = TextAlign.Center,
                color = SharedColors.ErrorRed,
                modifier = Modifier.padding(horizontal = 24.dp)

            )
        }
        Box(modifier = Modifier.padding(vertical = 12.dp)) {
            CodeEntry(
                length = TotpGenerator.CODE_LENGTH,
                enabled = !codeEditable,
                value = verificationCode,
                onValueChange = onVerificationCodeChanged,
                primaryColor = Color.Black,
                borderColor = SharedColors.BorderGrey,
                backgroundColor = SharedColors.WordBoxBackground,
                requestFocus = codeEditable
            )
        }

        LearnMore {

        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ApproveAccessInitialPreview() {
    ApproveAccessUI(
        storesLink = "link",
        approval = Approval(
            participantId = ParticipantId.generate(),
            status = ApprovalStatus.Initial
        ),
        verificationCode = "",
        onVerificationCodeChanged = {},
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ApproveAccessWaitingForVerificationPreview() {
    ApproveAccessUI(
        storesLink = "link",
        approval = Approval(
            participantId = ParticipantId.generate(),
            status = ApprovalStatus.WaitingForVerification
        ),
        verificationCode = "345819",
        onVerificationCodeChanged = {},
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ApproveAccessWaitingForApprovalPreview() {
    ApproveAccessUI(
        storesLink = "link",
        approval = Approval(
            participantId = ParticipantId.generate(),
            status = ApprovalStatus.WaitingForApproval
        ),
        verificationCode = "345819",
        onVerificationCodeChanged = {},
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ApproveAccessRejectedPreview() {
    ApproveAccessUI(
        storesLink = "link",
        approval = Approval(
            participantId = ParticipantId.generate(),
            status = ApprovalStatus.Rejected
        ),
        verificationCode = "345819",
        onVerificationCodeChanged = {},
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ApproveAccessApprovedPreview() {
    ApproveAccessUI(
        storesLink = "link",
        approval = Approval(
            participantId = ParticipantId.generate(),
            status = ApprovalStatus.Approved
        ),
        verificationCode = "345819",
        onVerificationCodeChanged = {},
    )
}