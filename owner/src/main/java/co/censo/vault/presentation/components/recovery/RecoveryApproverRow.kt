package co.censo.vault.presentation.components.recovery

import Base64EncodedData
import ParticipantId
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.data.cryptography.generatePartitionId
import co.censo.shared.data.cryptography.toHexString
import co.censo.shared.data.model.Approval
import co.censo.shared.data.model.ApprovalStatus
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import kotlinx.datetime.Clock

@Composable
fun RecoveryApprovalRow(
    guardian: Guardian.TrustedGuardian,
    approval: Approval
) {
    Row {
        Text(
            text = guardian.label,
            fontSize = 24.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = approval.status.name,
            fontSize = 24.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview
@Composable
fun RecoveryApprovalRowPreview() {
    val participantId = ParticipantId(generatePartitionId().toHexString())
    RecoveryApprovalRow(
        guardian = Guardian.TrustedGuardian(
            label = "Thomas Trusted",
            participantId = participantId,
            attributes = GuardianStatus.Onboarded(
                guardianEncryptedShard = Base64EncodedData(""), onboardedAt = Clock.System.now()
            )
        ),
        approval = Approval(
            participantId = participantId,
            status = ApprovalStatus.Initial
        )
    )
}