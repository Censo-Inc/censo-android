package co.censo.vault.presentation.components

import Base58EncodedGuardianPublicKey
import Base64EncodedData
import InvitationId
import ParticipantId
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.data.cryptography.generatePartitionId
import co.censo.shared.data.cryptography.toHexString
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.data.model.deeplink
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.components.TotpCodeView
import co.censo.vault.R
import co.censo.vault.presentation.VaultColors
import kotlinx.datetime.Clock

//region Top Bar
@Composable
fun ActivateApproversTopBar() {

    val context = LocalContext.current

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = VaultColors.PrimaryColor)
        ) {
            TextButton(
                modifier = Modifier.align(Alignment.TopEnd),
                onClick = {
                    Toast.makeText(
                        context,
                        "Take user to Security plan w/ editing on",
                        Toast.LENGTH_LONG
                    ).show()
                }) {
                Text(
                    text = stringResource(R.string.edit_plan),
                    color = Color.White
                )
            }
        }

        Box(
            modifier = Modifier
                .height(150.dp)
                .fillMaxWidth()
        ) {

            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(color = VaultColors.PrimaryColor)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(color = Color.White)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(
                            width = 5.dp, color = Color.Black, shape = CircleShape
                        )
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = "Censo Logo",
                        fontSize = 10.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
//endregion

//region Approver Row
@Composable
fun ActivateApproverRow(
    approver: Guardian,
    percentageLeft: Float = 0.0f,
    approverCode: String = "",
    horizontalPadding: Dp = 16.dp,
) {

    val context = LocalContext.current

    Column {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {

                val status = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = SharedColors.GreyText,
                            fontSize = 16.sp
                        )
                    ) {
                        append(stringResource(id = R.string.status))
                        append(": ")
                    }
                    this.appendApproverStatusText(
                        approver = approver,
                        context = context
                    )
                }

                Text(
                    text = status,
                    color = SharedColors.LabelText,
                    fontSize = 14.sp,
                )
                Text(
                    text = approver.label,
                    color = Color.Black,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.W700
                )
            }

            ActivateApproverActionItem(
                approver = approver,
                percentageLeft = percentageLeft,
                approverCode = approverCode,

            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Divider(
            modifier = Modifier.padding(start = horizontalPadding),
            thickness = 1.dp,
            color = SharedColors.DividerGray
        )
    }
}

@Composable
fun ActivateApproverActionItem(
    approver: Guardian,
    approverCode: String,
    percentageLeft: Float,
) {

    val context = LocalContext.current

    when (approver) {
        is Guardian.SetupGuardian -> {
            //A setup guardian should not be shown in list, but if they do, we show no action item.
        }

        is Guardian.TrustedGuardian -> {
            Icon(
                modifier = Modifier
                    .background(shape = CircleShape, color = SharedColors.SuccessGreen)
                    .padding(all = 8.dp),
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.approver_confirmed),
                tint = Color.White
            )
        }

        is Guardian.ProspectGuardian -> {
            when (approver.status) {
                is GuardianStatus.Accepted -> {
                    TotpCodeView(approverCode, percentageLeft.toInt(), VaultColors.PrimaryColor)
                }

                is GuardianStatus.Confirmed,
                is GuardianStatus.ImplicitlyOwner,
                is GuardianStatus.Onboarded -> {
                    Icon(
                        modifier = Modifier
                            .background(shape = CircleShape, color = SharedColors.SuccessGreen)
                            .padding(all = 8.dp),
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(id = R.string.approver_confirmed),
                        tint = Color.White
                    )
                }

                GuardianStatus.Declined -> {
                    Icon(
                        modifier = Modifier
                            .background(shape = CircleShape, color = Color.Red)
                            .padding(all = 8.dp),
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(R.string.approver_declined_invitation),
                        tint = Color.White
                    )
                }

                is GuardianStatus.Initial -> {
                    Icon(
                        modifier = Modifier
                            .background(shape = CircleShape, color = VaultColors.PrimaryColor)
                            .padding(all = 8.dp)
                            .clickable {
                                shareDeeplink(approver.deeplink(), context)
                            },
                        imageVector = Icons.Filled.IosShare,
                        contentDescription = stringResource(R.string.share_approver_invite_link),
                        tint = Color.White
                    )
                }


                is GuardianStatus.VerificationSubmitted -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = VaultColors.PrimaryColor,
                        strokeWidth = 3.dp,
                    )
                }
            }
        }
    }
}
//endregion

//region Smaller Composables/Utility Functions
fun AnnotatedString.Builder.appendApproverStatusText(context: Context, approver: Guardian) {

    val baseStyle =
        SpanStyle(
            fontWeight = FontWeight.W700,
            color = SharedColors.GreyText
        )

    when (approver) {
        is Guardian.SetupGuardian -> withStyle(baseStyle) {
            append(context.getString(R.string.pending))
        }

        is Guardian.ProspectGuardian -> {
            when (approver.status) {
                is GuardianStatus.Accepted ->
                    withStyle(baseStyle) {
                        append(context.getString(R.string.awaiting_code))
                    }

                is GuardianStatus.Confirmed,
                is GuardianStatus.ImplicitlyOwner,
                is GuardianStatus.Onboarded ->
                    withStyle(baseStyle.copy(color = SharedColors.SuccessGreen)) {
                        append(context.getString(R.string.completed))
                    }

                GuardianStatus.Declined -> withStyle(baseStyle.copy(color = Color.Red)) {
                    append(context.getString(R.string.declined))
                }

                is GuardianStatus.Initial -> withStyle(baseStyle) {
                    append(context.getString(R.string.pending))
                }

                is GuardianStatus.VerificationSubmitted -> withStyle(baseStyle) {
                    append(context.getString(R.string.code_submitted))
                }
            }
        }

        is Guardian.TrustedGuardian -> {
            withStyle(baseStyle.copy(color = SharedColors.SuccessGreen)) {
                append(context.getString(R.string.completed))
            }
        }
    }
}

fun shareDeeplink(deeplink: String, context: Context) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, deeplink)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}
//endregion

//region Dummy Data
val dummyListOfApprovers = listOf(
    Guardian.ProspectGuardian(
        invitationId = InvitationId("12345"),
        "Anton",
        participantId = ParticipantId(generatePartitionId().toHexString()),
        status = GuardianStatus.Initial(
            deviceEncryptedTotpSecret = Base64EncodedData(""),
        )
    ),
    Guardian.ProspectGuardian(
        invitationId = InvitationId("12345"),
        "Ata",
        participantId = ParticipantId(generatePartitionId().toHexString()),
        status = GuardianStatus.Accepted(
            acceptedAt = Clock.System.now(),
            deviceEncryptedTotpSecret = Base64EncodedData(""),
        )
    ),
    Guardian.ProspectGuardian(
        invitationId = InvitationId("12345"),
        "Ben",
        participantId = ParticipantId(generatePartitionId().toHexString()),
        status = GuardianStatus.VerificationSubmitted(
            signature = Base64EncodedData(""),
            timeMillis = 1L,
            guardianPublicKey = Base58EncodedGuardianPublicKey(""),
            submittedAt = Clock.System.now(),
            deviceEncryptedTotpSecret = Base64EncodedData(""),
        )
    ),
    Guardian.ProspectGuardian(
        invitationId = InvitationId("12345"),
        "Brendan",
        participantId = ParticipantId(generatePartitionId().toHexString()),
        status = GuardianStatus.Confirmed(
            guardianKeySignature = Base64EncodedData(""),
            guardianPublicKey = Base58EncodedGuardianPublicKey(""),
            timeMillis = 1L,
            confirmedAt = Clock.System.now()
        )
    ),
    Guardian.ProspectGuardian(
        invitationId = InvitationId("12345"),
        "Charlie",
        participantId = ParticipantId(generatePartitionId().toHexString()),
        status = GuardianStatus.Declined
    ),
)
//endregion