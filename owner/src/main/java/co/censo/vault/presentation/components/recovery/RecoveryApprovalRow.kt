package co.censo.vault.presentation.components.recovery

import Base64EncodedData
import ParticipantId
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import co.censo.shared.presentation.SharedColors
import co.censo.vault.R
import co.censo.vault.presentation.VaultColors
import co.censo.vault.presentation.components.shareDeeplink
import kotlinx.datetime.Clock

@Composable
fun RecoveryApprovalRow(
    guardian: Guardian.TrustedGuardian,
    approval: Approval,
    onEnterCode: (Approval) -> Unit
) {

    val context = LocalContext.current

    val recoveryStatusRowStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.W700,
    )

    Row {
        Column {
            Row {
                Text(
                    text = stringResource(R.string.status_colon),
                    style = recoveryStatusRowStyle
                        .copy(fontWeight = FontWeight.W400),
                    color = SharedColors.BrandGray,
                )

                Spacer(modifier = Modifier.width(4.dp))

                when (approval.status) {
                    ApprovalStatus.Initial -> {
                        Text(
                            text = stringResource(R.string.pending),
                            style = recoveryStatusRowStyle
                                .copy(color = SharedColors.BrandGray)
                        )
                    }

                    ApprovalStatus.WaitingForVerification -> {
                        Text(
                            text = stringResource(R.string.awaiting_code),
                            style = recoveryStatusRowStyle
                                .copy(color = Color.White)
                        )
                    }

                    ApprovalStatus.WaitingForApproval -> {
                        Text(
                            text = stringResource(R.string.verifying),
                            style = recoveryStatusRowStyle
                                .copy(color = Color.White)
                        )
                    }

                    ApprovalStatus.Approved -> {
                        Text(
                            text = stringResource(R.string.approved),
                            style = recoveryStatusRowStyle
                                .copy(color = SharedColors.SuccessGreen)
                        )
                    }

                    ApprovalStatus.Rejected -> {
                        Text(
                            text = stringResource(R.string.incorrect_code),
                            style = recoveryStatusRowStyle
                                .copy(color = SharedColors.ErrorRed)
                        )
                    }
                }
            }

            Text(
                text = guardian.label,
                fontSize = 24.sp,
                color = Color.White,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        when (approval.status) {
            ApprovalStatus.Initial -> {
                IconButton(
                    onClick = {
                        shareDeeplink(approval.deepLink(), context)
                    }
                ) {
                    Icon(
                        modifier = Modifier
                            .background(shape = CircleShape, color = Color.White)
                            .padding(all = 8.dp),
                        imageVector = Icons.Outlined.IosShare,
                        contentDescription = stringResource(R.string.share_approver_recovery_link),
                        tint = VaultColors.PrimaryColor
                    )
                }
            }

            ApprovalStatus.WaitingForVerification,
            ApprovalStatus.Rejected -> {
                TextButton(
                    modifier = Modifier
                        .padding(all = 10.dp)
                        .background(color = Color.White, shape = RoundedCornerShape(4.dp))
                        .height(24.dp)
                        .align(alignment = Alignment.CenterVertically),

                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    onClick = { onEnterCode(approval) }
                ) {
                    Text(
                        text = stringResource(R.string.enter_code),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.W400,
                        color = VaultColors.PrimaryColor,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            ApprovalStatus.WaitingForApproval -> {

            }

            ApprovalStatus.Approved -> {
                Icon(
                    modifier = Modifier
                        .background(shape = CircleShape, color = SharedColors.SuccessGreen)
                        .padding(all = 8.dp),
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(R.string.recovery_approved),
                    tint = Color.White
                )
            }
        }
    }
}

@Preview
@Composable
fun RecoveryApprovalRowPreview() {
    val participantId = ParticipantId(generatePartitionId().toHexString())
    Box(modifier = Modifier.background(color = VaultColors.PrimaryColor)) {
        RecoveryApprovalRow(
            guardian = Guardian.TrustedGuardian(
                label = "Thomas Trusted",
                participantId = participantId,
                attributes = GuardianStatus.Onboarded(
                    onboardedAt = Clock.System.now()
                )
            ),
            approval = Approval(
                participantId = participantId,
                status = ApprovalStatus.WaitingForVerification
            ),
            onEnterCode = {}
        )
    }
}