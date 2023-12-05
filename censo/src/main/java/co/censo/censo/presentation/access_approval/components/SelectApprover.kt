package co.censo.censo.presentation.access_approval.components

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
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.presentation.SharedColors
import co.censo.censo.R
import co.censo.shared.data.model.RecoveryIntent
import kotlinx.datetime.Clock

@Composable
fun SelectApprover(
    intent: RecoveryIntent,
    approvers: List<Guardian.TrustedGuardian>,
    selectedApprover: Guardian.TrustedGuardian?,
    onApproverSelected: (Guardian.TrustedGuardian) -> Unit,
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 36.dp),
                title = stringResource(
                    when (intent) {
                        RecoveryIntent.AccessPhrases -> R.string.request_access
                        RecoveryIntent.ReplacePolicy -> R.string.request_approval
                    }
                ),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(verticalSpacingHeight - 8.dp))

            MessageText(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 36.dp),
                message = stringResource(
                    when (intent) {
                        RecoveryIntent.AccessPhrases -> R.string.seed_phrase_request_access_message
                        RecoveryIntent.ReplacePolicy -> R.string.policy_replace_request_access_message
                    },
                    buildApproverNamesText(approvers = approvers, context)
                ),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(24.dp))

            approvers.sortedBy { it.attributes.onboardedAt }.forEachIndexed { _, approver ->
                Spacer(modifier = Modifier.height(12.dp))

                SelectingApproverInfoBox(
                    nickName = approver.label,
                    selected = (approver == selectedApprover),
                    onSelect = { onApproverSelected(approver) }
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
                        disabledColor = SharedColors.DisabledGrey,
                        color = Color.Black,
                        onClick = onContinue,
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 32.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.continue_text),
                            color = if (buttonEnabled) Color.White else SharedColors.DisabledFontGrey,
                            fontSize = 24.sp
                        )
                    }
                }
            }
        }
    }
}

fun buildApproverNamesText(approvers: List<Guardian.TrustedGuardian>, context: Context): String {
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
                color = if (selected) Color.Black else SharedColors.BorderGrey,
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
            if (selected) {
                Icon(
                    painterResource(id = co.censo.shared.R.drawable.check_icon),
                    contentDescription = stringResource(R.string.select_approver),
                    tint = Color.Black
                )
            }
        }

        Column {
            Text(
                text = stringResource(id = R.string.approver),
                color = Color.Black,
                fontSize = labelTextSize
            )

            Text(
                text = nickName,
                color = Color.Black,
                fontSize = 24.sp
            )

            Text(
                text = context.getString(R.string.active),
                color = SharedColors.SuccessGreen,
                fontSize = labelTextSize
            )

        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SelectApproverForAccessUIPreview() {
    val primaryApprover = Guardian.TrustedGuardian(
        label = "Neo",
        participantId = ParticipantId.generate(),
        isOwner = false,
        attributes = GuardianStatus.Onboarded(
            onboardedAt = Clock.System.now(),
        )
    )
    val backupApprover = Guardian.TrustedGuardian(
        label = "John Wick",
        participantId = ParticipantId.generate(),
        isOwner = false,
        attributes = GuardianStatus.Onboarded(
            onboardedAt = Clock.System.now(),
        )
    )

    SelectApprover(
        intent = RecoveryIntent.AccessPhrases,
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
    val primaryApprover = Guardian.TrustedGuardian(
        label = "Neo",
        participantId = ParticipantId.generate(),
        isOwner = false,
        attributes = GuardianStatus.Onboarded(
            onboardedAt = Clock.System.now(),
        )
    )
    val backupApprover = Guardian.TrustedGuardian(
        label = "John Wick",
        participantId = ParticipantId.generate(),
        isOwner = false,
        attributes = GuardianStatus.Onboarded(
            onboardedAt = Clock.System.now(),
        )
    )

    SelectApprover(
        intent = RecoveryIntent.ReplacePolicy,
        approvers = listOf(
            primaryApprover,
            backupApprover
        ),
        selectedApprover = backupApprover,
        onApproverSelected = {},
        onContinue = {}
    )
}