package co.censo.censo.presentation.biometry_reset.components

import MessageText
import ParticipantId
import StandardButton
import TitleText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.ApproverStatus
import co.censo.censo.R
import co.censo.censo.presentation.access_approval.components.SelectingApproverInfoBox
import co.censo.censo.presentation.access_approval.components.buildApproverNamesText
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.DisabledButtonTextStyle
import co.censo.shared.data.model.AuthenticationResetApproval
import co.censo.shared.data.model.AuthenticationResetApprovalStatus
import kotlinx.datetime.Clock

@Composable
fun SelectBiometryResetApproverUI(
    approvals: List<AuthenticationResetApproval>,
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
                title = "Biometry reset",
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(verticalSpacingHeight - 8.dp))

            MessageText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp),
                message = stringResource(
                    R.string.biometry_reset_message,
                    buildApproverNamesText(approvers, context)
                ),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(24.dp))

            approvers.sortedBy { it.attributes.onboardedAt }.forEachIndexed { _, approver ->
                Spacer(modifier = Modifier.height(12.dp))

                val approved = approvals.any { it.participantId == approver.participantId && it.status == AuthenticationResetApprovalStatus.Approved }
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SelectBiometryResetApproverUIPreview() {
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

    SelectBiometryResetApproverUI(
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
