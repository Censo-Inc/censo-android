package co.censo.censo.presentation.access_approval.components

import ApprovalId
import MessageText
import ParticipantId
import StandardButton
import TitleText
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.sp
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.ApproverStatus
import co.censo.shared.presentation.SharedColors
import co.censo.censo.R
import co.censo.shared.data.model.AccessIntent
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.DisabledButtonTextStyle
import co.censo.shared.data.model.Approval
import co.censo.shared.data.model.ApprovalStatus
import kotlinx.datetime.Clock

@Composable
fun SelectApprover(
    intent: AccessIntent,
    approvals: List<Approval>,
    approvers: List<Approver.TrustedApprover>,
    selectedApprover: Approver.TrustedApprover?,
    onApproverSelected: (Approver.TrustedApprover) -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val buttonEnabled = selectedApprover != null
    val verticalSpacingHeight = 28.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            TitleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp),
                title = stringResource(
                    when (intent) {
                        AccessIntent.AccessPhrases -> R.string.request_access
                        AccessIntent.ReplacePolicy,
                        AccessIntent.RecoverOwnerKey -> R.string.request_approval
                    }
                ),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(verticalSpacingHeight - 8.dp))

            MessageText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp),
                message = stringResource(
                    when (intent) {
                        AccessIntent.AccessPhrases -> R.string.seed_phrase_request_access_message
                        AccessIntent.ReplacePolicy -> R.string.policy_replace_request_access_message
                        AccessIntent.RecoverOwnerKey -> R.string.recover_key_request_access_message
                    },
                    buildApproverNamesText(intent, approvers, context)
                ),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(24.dp))

            approvers.sortedBy { it.attributes.onboardedAt }.forEachIndexed { _, approver ->
                Spacer(modifier = Modifier.height(12.dp))

                val approved = approvals.any { it.participantId == approver.participantId && it.status == ApprovalStatus.Approved }
                val selected = approver == selectedApprover
                SelectingApproverInfoBox(
                    nickName = approver.label,
                    selected = selected,
                    approved = approved,
                    onSelect = { if (!approved) onApproverSelected(approver) }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Divider()

                    Spacer(modifier = Modifier.height(24.dp))

                    StandardButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 36.dp),
                        enabled = buttonEnabled,
                        onClick = onContinue,
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 32.dp)
                    ) {
                        val continueButtonTextStyle = if (buttonEnabled) ButtonTextStyle else DisabledButtonTextStyle

                        Text(
                            text = stringResource(id = R.string.continue_text),
                            style = continueButtonTextStyle.copy(fontSize = 20.sp)
                        )
                    }
                }
            }
        }
    }
}

fun buildApproverNamesText(
    intent: AccessIntent,
    approvers: List<Approver.TrustedApprover>,
    context: Context,
): String {
    val primaryApproverName =
        approvers.sortedBy { it.attributes.onboardedAt }.getOrNull(0)?.label ?: ""
    val alternateApproverName =
        approvers.sortedBy { it.attributes.onboardedAt }.getOrNull(1)?.label ?: ""

    val primaryApproverNameNotEmpty = primaryApproverName.isNotEmpty()
    val bothApproverNamesNotEmpty =
        primaryApproverNameNotEmpty && alternateApproverName.isNotEmpty()

    return if (approvers.size == 1 && primaryApproverNameNotEmpty) {
        primaryApproverName
    } else if (approvers.size == 2 && bothApproverNamesNotEmpty) {
        "$primaryApproverName or $alternateApproverName"
    } else {
        context.getString(R.string.your_approver)
    }
}

@Composable
fun SelectingApproverInfoBox(
    nickName: String,
    selected: Boolean,
    approved: Boolean,
    onSelect: () -> Unit
) {

    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp)
            .background(
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent
            )
            .border(
                width = 1.dp,
                color = if (approved) SharedColors.DisabledGrey else if (selected) SharedColors.MainBorderColor else SharedColors.BorderGrey,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clickable { onSelect() },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {

        val labelTextSize = 14.sp

        Box(
            modifier = Modifier
                .width(25.dp)
                .align(Alignment.CenterVertically),
        ) {
            if (selected || approved) {
                Icon(
                    painterResource(id = co.censo.shared.R.drawable.check_icon),
                    contentDescription = stringResource(R.string.select_approver),
                    tint = if (approved) SharedColors.DisabledGrey else SharedColors.MainIconColor
                )
            }
        }

        Column {
            Text(
                text = stringResource(id = R.string.approver),
                color = if (approved) SharedColors.GreyText else SharedColors.MainColorText,
                fontSize = labelTextSize
            )

            Text(
                text = nickName,
                color = if (approved) SharedColors.GreyText else SharedColors.MainColorText,
                fontSize = 24.sp
            )

            Text(
                text = context.getString(R.string.active),
                color = if (approved) SharedColors.GreyText else SharedColors.SuccessGreen,
                fontSize = labelTextSize
            )

        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SelectApproverForAccessUIPreview() {
    val primaryApprover = Approver.TrustedApprover(
        label = "Neo",
        participantId = ParticipantId.generate(),
        isOwner = false,
        attributes = ApproverStatus.Onboarded(
            onboardedAt = Clock.System.now(),
        )
    )
    val backupApprover = Approver.TrustedApprover(
        label = "John Wick",
        participantId = ParticipantId.generate(),
        isOwner = false,
        attributes = ApproverStatus.Onboarded(
            onboardedAt = Clock.System.now(),
        )
    )

    SelectApprover(
        intent = AccessIntent.AccessPhrases,
        approvals = listOf(),
        approvers = listOf(
            primaryApprover,
            backupApprover
        ),
        selectedApprover = backupApprover,
        onApproverSelected = {},
        onContinue = {}
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SelectApproverForPolicyReplaceUIPreview() {
    val primaryApprover = Approver.TrustedApprover(
        label = "Neo",
        participantId = ParticipantId.generate(),
        isOwner = false,
        attributes = ApproverStatus.Onboarded(
            onboardedAt = Clock.System.now(),
        )
    )
    val backupApprover = Approver.TrustedApprover(
        label = "John Wick",
        participantId = ParticipantId.generate(),
        isOwner = false,
        attributes = ApproverStatus.Onboarded(
            onboardedAt = Clock.System.now(),
        )
    )

    SelectApprover(
        intent = AccessIntent.ReplacePolicy,
        approvals = listOf(),
        approvers = listOf(
            primaryApprover,
            backupApprover
        ),
        selectedApprover = backupApprover,
        onApproverSelected = {},
        onContinue = {}
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SelectApproverForKeyRecoveryUIPreview() {
    val primaryApprover = Approver.TrustedApprover(
        label = "Neo",
        participantId = ParticipantId.generate(),
        isOwner = false,
        attributes = ApproverStatus.Onboarded(
            onboardedAt = Clock.System.now(),
        )
    )
    val backupApprover = Approver.TrustedApprover(
        label = "John Wick",
        participantId = ParticipantId.generate(),
        isOwner = false,
        attributes = ApproverStatus.Onboarded(
            onboardedAt = Clock.System.now(),
        )
    )

    SelectApprover(
        intent = AccessIntent.RecoverOwnerKey,
        approvals = listOf(
            Approval(
                approvalId = ApprovalId("1"),
                participantId = primaryApprover.participantId,
                status = ApprovalStatus.Approved
            )
        ),
        approvers = listOf(
            primaryApprover,
            backupApprover
        ),
        selectedApprover = backupApprover,
        onApproverSelected = {},
        onContinue = {}
    )
}