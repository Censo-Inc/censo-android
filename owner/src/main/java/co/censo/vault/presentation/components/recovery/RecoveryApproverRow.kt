package co.censo.vault.presentation.components.recovery

import Base64EncodedData
import ParticipantId
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.data.cryptography.generatePartitionId
import co.censo.shared.data.cryptography.toHexString
import co.censo.shared.data.model.Approval
import co.censo.shared.data.model.ApprovalStatus
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import co.censo.vault.presentation.VaultColors
import kotlinx.datetime.Clock

@Composable
fun RecoveryApprovalRow(
    guardian: Guardian.TrustedGuardian,
    approval: Approval
) {

    Row(
        //modifier = Modifier.padding(all = 12.dp)
    ) {
        Column {
            Row {
                Text(
                    text = "Status: ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W300,
                    color = Color.White,
                )

                Text(
                    text = approval.status.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W700,
                    color = Color.White,
                )
            }

            //Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = guardian.label,
                fontSize = 24.sp,
                color = Color.White,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            modifier = Modifier.background(
                color = Color.White,
                shape = CircleShape
            ),

            onClick = { }
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = Icons.Outlined.IosShare,
                contentDescription = "Share code",
                tint = VaultColors.PrimaryColor
            )
        }
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