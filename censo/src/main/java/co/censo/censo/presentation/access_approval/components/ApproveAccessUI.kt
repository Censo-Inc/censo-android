package co.censo.censo.presentation.access_approval.components

import ApprovalId
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.model.Approval
import co.censo.shared.data.model.ApprovalStatus
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.components.CodeEntry
import co.censo.censo.R
import co.censo.censo.presentation.plan_setup.components.ApproverStep
import co.censo.shared.presentation.components.Loading
import co.censo.shared.presentation.components.SmallLoading

@Composable
fun ApproveAccessUI(
    approverName: String,
    approval: Approval,
    verificationCode: String,
    onVerificationCodeChanged: (String) -> Unit,
    storesLink: String, //This should contain both links since approver could be either
) {
    val linkTag = "link"

    val deeplink = approval.v2Deeplink()
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
        verticalArrangement = Arrangement.Top,
    ) {

        Spacer(modifier = Modifier.height(24.dp))

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = R.string.request_access,
            fontSize = 28.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        MessageText(
            modifier = Modifier.fillMaxWidth(),
            message = R.string.you_must_do_two_things,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        val firstStepContentSpan = buildAnnotatedString {
            append(stringResource(R.string.share_this_link_and_have_span))
            append(" $approverName ")
            append(stringResource(R.string.tap_on_it_or_paste_it_into_the_censo_approver_app_if_span))
            append(" $approverName ")
            append(stringResource(R.string.no_longer_has_the_app_installed_it_can_be_span))

            pushStringAnnotation(tag = linkTag, annotation = storesLink)
            withStyle(style = SpanStyle(fontWeight = FontWeight.W600)) {
                append(stringResource(R.string.downloaded_here_url_span))
            }
            pop()
        }

        ApproverStep(
            imagePainter = painterResource(id = co.censo.shared.R.drawable.share_link),
            heading = stringResource(R.string.share_this_link_request_access),
            content = firstStepContentSpan,
            stringAnnotationTag = linkTag,
            onShareLink = { shareLink(deeplink) },
        )

        Spacer(modifier = Modifier.height(12.dp))

        ApproverStep(
            imagePainter = painterResource(id = co.censo.shared.R.drawable.phrase_entry),
            heading = stringResource(R.string.enter_the_code_request_access_step),
            content = stringResource(
                R.string.have_read_aloud_the_6_digit_code_from_the_censo_approver_app_and_enter_it_below,
                approverName
            ),
        )

        if (approval.status == ApprovalStatus.Initial) {
            Spacer(modifier = Modifier.height(24.dp))
            Loading(
                strokeWidth = 3.5.dp,
                size = 24.dp,
                fullscreen = false,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.waiting_for_to_open_the_link, approverName),
                color = SharedColors.GreyText
            )
        } else {
            if (approval.status == ApprovalStatus.Rejected) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.the_code_you_entered_is_not_correct),
                    textAlign = TextAlign.Center,
                    color = SharedColors.ErrorRed,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (approval.status == ApprovalStatus.WaitingForVerification) {
                Spacer(modifier = Modifier.height(24.dp))
            }

            Box(modifier = Modifier.padding(vertical = 12.dp)) {
                CodeEntry(
                    length = TotpGenerator.CODE_LENGTH,
                    enabled = codeEditable,
                    value = verificationCode,
                    onValueChange = onVerificationCodeChanged,
                    primaryColor = Color.Black,
                    borderColor = SharedColors.BorderGrey,
                    backgroundColor = SharedColors.WordBoxBackground,
                    requestFocus = codeEditable
                )
            }

            if (approval.status == ApprovalStatus.WaitingForApproval) {
                Spacer(modifier = Modifier.height(6.dp))
                SmallLoading(
                    fullscreen = false,
                )
            }
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ApproveAccessInitialPreview() {
    ApproveAccessUI(
        approverName = "Buddy",
        storesLink = "link",
        approval = Approval(
            participantId = ParticipantId.generate(),
            status = ApprovalStatus.Initial,
            approvalId = ApprovalId("")
        ),
        verificationCode = "",
        onVerificationCodeChanged = {},
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ApproveAccessWaitingForVerificationPreview() {
    ApproveAccessUI(
        approverName = "Buddy",
        storesLink = "link",
        approval = Approval(
            participantId = ParticipantId.generate(),
            status = ApprovalStatus.WaitingForVerification,
            approvalId = ApprovalId("")
        ),
        verificationCode = "345819",
        onVerificationCodeChanged = {},
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ApproveAccessWaitingForApprovalPreview() {
    ApproveAccessUI(
        approverName = "Buddy",
        storesLink = "link",
        approval = Approval(
            participantId = ParticipantId.generate(),
            status = ApprovalStatus.WaitingForApproval,
            approvalId = ApprovalId("")
        ),
        verificationCode = "345819",
        onVerificationCodeChanged = {},
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ApproveAccessRejectedPreview() {
    ApproveAccessUI(
        approverName = "Buddy",
        storesLink = "link",
        approval = Approval(
            participantId = ParticipantId.generate(),
            status = ApprovalStatus.Rejected,
            approvalId = ApprovalId("")
        ),
        verificationCode = "345819",
        onVerificationCodeChanged = {},
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ApproveAccessApprovedPreview() {
    ApproveAccessUI(
        approverName = "Buddy",
        storesLink = "link",
        approval = Approval(
            participantId = ParticipantId.generate(),
            status = ApprovalStatus.Approved,
            approvalId = ApprovalId("")
        ),
        verificationCode = "345819",
        onVerificationCodeChanged = {},
    )
}