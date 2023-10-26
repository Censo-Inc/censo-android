package co.censo.vault.presentation.components.recovery

import ParticipantId
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.data.cryptography.generatePartitionId
import co.censo.shared.data.cryptography.toHexString
import co.censo.shared.data.model.Approval
import co.censo.shared.data.model.ApprovalStatus
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.data.model.Recovery
import co.censo.shared.data.model.RecoveryId
import co.censo.shared.data.model.RecoveryStatus
import co.censo.vault.R
import co.censo.vault.presentation.VaultColors
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days

@Composable
fun ThisDeviceRecoveryScreen(
    recovery: Recovery.ThisDevice,
    guardians: List<Guardian.TrustedGuardian>,
    approvalsCollected: Int,
    approvalsRequired: Int,
    onCancelRecovery: () -> Unit,
    onEnterCode: (Approval) -> Unit
) {

    Column(
        Modifier
            .fillMaxSize()
            .background(color = VaultColors.PrimaryColor)
            .padding(all = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        ActiveRecoveryTopBar(
            recovery,
            approvalsCollected,
            approvalsRequired,
            onCancelRecovery,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            textAlign = TextAlign.Center,
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = Color.White,
                        fontSize = 14.sp
                    )
                ) {
                    append(stringResource(R.string.tap_the))
                    append(" ")
                    appendInlineContent("[icon]", "[icon]")
                    append(" ")
                    append(stringResource(R.string.icon_next_to_each_of_your_approvers_to_send_them_the_recovery_link))
                }
            },
            inlineContent = mapOf(
                Pair(
                    "[icon]",
                    InlineTextContent(
                        Placeholder(
                            width = 16.sp,
                            height = 16.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline
                        )
                    ) {
                        Icon(
                            modifier = Modifier.size(16.dp),
                            imageVector = Icons.Outlined.IosShare,
                            contentDescription = stringResource(R.string.share_approver_recovery_link),
                            tint = Color.White
                        )
                    }
                )
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier
                .background(
                    Color.White.copy(alpha = 0.05f),
                    RoundedCornerShape(10.dp)
                )
                .padding(all = 16.dp)
        ) {
            val approvals = recovery.approvals

            items(approvals.size) { index ->
                val approval = approvals[index]
                RecoveryApprovalRow(
                    guardian = guardians.first { it.participantId == approval.participantId },
                    approval = approval,
                    onEnterCode = onEnterCode
                )

                if (index != approvals.size - 1)
                    Divider(
                        color = Color.White.copy(alpha = 0.1f),
                        thickness = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
            }
        }
    }
}

@Preview
@Composable
fun ThisDeviceRecoveryScreenPreview() {
    val participants = listOf(
        ParticipantId(generatePartitionId().toHexString()),
        ParticipantId(generatePartitionId().toHexString()),
        ParticipantId(generatePartitionId().toHexString()),
        ParticipantId(generatePartitionId().toHexString()),
        ParticipantId(generatePartitionId().toHexString()),
    )

    val approvals = participants.mapIndexed { index, participantId ->
        Approval(
            participantId = participantId,
            status = ApprovalStatus.values()[index]
        )
    }.toList()

    val guardians = participants.mapIndexed { index, participantId ->
        Guardian.TrustedGuardian(
            label = "Approver $index",
            participantId = participantId,
            isOwner = false,
            attributes = GuardianStatus.Onboarded(
                onboardedAt = Clock.System.now(),
            )
        )
    }.toList()

    ThisDeviceRecoveryScreen(
        recovery = Recovery.ThisDevice(
            guid = RecoveryId("recovery id"),
            status = RecoveryStatus.Requested,
            createdAt = Clock.System.now(),
            unlocksAt = Clock.System.now(),
            expiresAt = Clock.System.now() + 1.days,
            approvals = approvals
        ),
        guardians = guardians,
        approvalsCollected = 1,
        approvalsRequired = 3,
        onCancelRecovery = { },
        onEnterCode = { }
    )
}