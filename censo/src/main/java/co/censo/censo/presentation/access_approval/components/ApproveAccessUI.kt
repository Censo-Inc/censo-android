package co.censo.censo.presentation.access_approval.components

import ApprovalId
import TitleText
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.model.AccessApproval
import co.censo.shared.data.model.AccessApprovalStatus
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.components.CodeEntry
import co.censo.censo.R
import co.censo.censo.presentation.components.ApproverStep
import co.censo.shared.data.model.AccessIntent
import co.censo.shared.presentation.components.KeepScreenOn
import co.censo.shared.presentation.components.Loading
import co.censo.shared.presentation.components.SmallLoading

@Composable
fun ApproveAccessUI(
    intent: AccessIntent,
    approverName: String,
    approval: AccessApproval,
    verificationCode: String,
    onVerificationCodeChanged: (String) -> Unit,
) {
    val deeplink = approval.v2Deeplink()
    val codeEditable = approval.status in listOf(AccessApprovalStatus.WaitingForVerification, AccessApprovalStatus.Rejected)

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
            title = when (intent) {
                AccessIntent.AccessPhrases -> R.string.request_access
                AccessIntent.ReplacePolicy -> R.string.request_approval
                AccessIntent.RecoverOwnerKey -> R.string.recover_key
            },
            fontSize = 28.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        ApproverStep(
            heading = listOf(
                stringResource(id = R.string.step_1),
                stringResource(id = R.string.share_this_link_title),
            ),
            content = stringResource(R.string.share_link_access_message, approverName),
            onClick = { shareLink(deeplink) }
        ) {
            Image(
                modifier = Modifier.size(44.dp),
                painter = painterResource(id = R.drawable.censo_login_logo),
                contentDescription = null
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ApproverStep(
            heading = listOf(
                stringResource(id = R.string.step_2),
                stringResource(id = R.string.enter_the_code_title),
            ),
            content = stringResource(
                R.string.enter_code_access_message,
                approverName,
            ),
            includeLine = false
        ) {
            Image(
                modifier = Modifier.size(44.dp),
                painter = painterResource(id = co.censo.shared.R.drawable.phonewaveform),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color = SharedColors.MainIconColor)
            )
        }

        if (approval.status == AccessApprovalStatus.Initial) {
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
            if (approval.status == AccessApprovalStatus.Rejected) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.the_code_you_entered_is_not_correct),
                    textAlign = TextAlign.Center,
                    color = SharedColors.ErrorRed,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (approval.status == AccessApprovalStatus.WaitingForVerification) {
                Spacer(modifier = Modifier.height(24.dp))
            }

            Box(modifier = Modifier.padding(vertical = 12.dp)) {
                CodeEntry(
                    length = TotpGenerator.CODE_LENGTH,
                    enabled = codeEditable,
                    value = verificationCode,
                    onValueChange = onVerificationCodeChanged,
                    primaryColor = SharedColors.MainColorText,
                    borderColor = SharedColors.BorderGrey,
                    backgroundColor = SharedColors.WordBoxBackground,
                    requestFocus = codeEditable
                )
            }

            if (approval.status == AccessApprovalStatus.WaitingForApproval) {
                Spacer(modifier = Modifier.height(6.dp))
                SmallLoading(
                    fullscreen = false,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.waiting_for_to_verify_the_code, approverName),
                    color = SharedColors.MainColorText,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
    KeepScreenOn()
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ApproveAccessInitialPreview() {
    ApproveAccessUI(
        intent = AccessIntent.AccessPhrases,
        approverName = "Buddy",
        approval = AccessApproval(
            participantId = ParticipantId.generate(),
            status = AccessApprovalStatus.Initial,
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
        intent = AccessIntent.AccessPhrases,
        approverName = "Buddy",
        approval = AccessApproval(
            participantId = ParticipantId.generate(),
            status = AccessApprovalStatus.WaitingForVerification,
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
        intent = AccessIntent.AccessPhrases,
        approverName = "Buddy",
        approval = AccessApproval(
            participantId = ParticipantId.generate(),
            status = AccessApprovalStatus.WaitingForApproval,
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
        intent = AccessIntent.AccessPhrases,
        approverName = "Buddy",
        approval = AccessApproval(
            participantId = ParticipantId.generate(),
            status = AccessApprovalStatus.Rejected,
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
        intent = AccessIntent.AccessPhrases,
        approverName = "Buddy",
        approval = AccessApproval(
            participantId = ParticipantId.generate(),
            status = AccessApprovalStatus.Approved,
            approvalId = ApprovalId("")
        ),
        verificationCode = "345819",
        onVerificationCodeChanged = {},
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ApproveRequestInitialPreview() {
    ApproveAccessUI(
        intent = AccessIntent.ReplacePolicy,
        approverName = "Buddy",
        approval = AccessApproval(
            participantId = ParticipantId.generate(),
            status = AccessApprovalStatus.Initial,
            approvalId = ApprovalId("")
        ),
        verificationCode = "",
        onVerificationCodeChanged = {},
    )
}