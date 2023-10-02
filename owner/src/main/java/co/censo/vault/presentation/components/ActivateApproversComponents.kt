package co.censo.vault.presentation.components

import Base58EncodedGuardianPublicKey
import Base64EncodedData
import InvitationId
import ParticipantId
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.data.cryptography.generatePartitionId
import co.censo.shared.data.cryptography.toHexString
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.data.model.VerificationStatus
import co.censo.shared.presentation.Colors
import co.censo.vault.R
import kotlinx.datetime.Clock

//region Top Bar
@Composable
fun ActivateApproversTopBar() {

    val context = LocalContext.current

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Colors.PrimaryBlue)
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
                    text = "Edit plan",
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
                        .background(color = Colors.PrimaryBlue)
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
    approver: Guardian.ProspectGuardian,
    horizontalPadding: Dp = 16.dp,
    actionClick: () -> Unit
) {
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
                            color = Colors.GreyText,
                            fontSize = 16.sp
                        )
                    ) {
                        append(stringResource(id = R.string.status))
                        append(": ")
                    }
                    this.appendGuardianStatusText(approver.status)
                }

                Text(
                    text = status,
                    color = Colors.LabelText,
                    fontSize = 14.sp,
                )
                Text(
                    text = approver.label,
                    color = Color.Black,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.W700
                )
            }

            ActivateApproverActionItem(guardianStatus = approver.status)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Divider(
            modifier = Modifier.padding(start = horizontalPadding),
            thickness = 1.dp,
            color = Colors.DividerGray
        )
    }
}

@Composable
fun ActivateApproverActionItem(guardianStatus: GuardianStatus) {
    when (guardianStatus) {
        is GuardianStatus.Accepted -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "123-456",
                    color = Colors.PrimaryBlue,
                    fontWeight = FontWeight.W600,
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = Colors.TimeLeftGray,
                            shape = CircleShape
                        )
                        .background(
                            color = Color.White,
                            shape = TimeLeftShape(0.75f)
                        )
                )
            }
        }

        is GuardianStatus.Confirmed,
        is GuardianStatus.Onboarded -> {
            Icon(
                modifier = Modifier
                    .background(shape = CircleShape, color = Colors.SuccessGreen)
                    .padding(all = 8.dp)
                    .clickable { },
                imageVector = Icons.Filled.Check,
                contentDescription = "approver confirmed",
                tint = Color.White
            )
        }

        GuardianStatus.Declined -> {
            Icon(
                modifier = Modifier
                    .background(shape = CircleShape, color = Color.Red)
                    .padding(all = 8.dp)
                    .clickable { },
                imageVector = Icons.Filled.Clear,
                contentDescription = "approver declined invitation",
                tint = Color.White
            )
        }

        is GuardianStatus.Invited,
        GuardianStatus.Initial -> {
            Icon(
                modifier = Modifier
                    .background(shape = CircleShape, color = Colors.PrimaryBlue)
                    .padding(all = 8.dp)
                    .clickable { },
                imageVector = Icons.Filled.IosShare,
                contentDescription = "share approver invite link",
                tint = Color.White
            )
        }


        is GuardianStatus.VerificationSubmitted -> {
            Button(onClick = { }) {
                Text(text = "Verify Code", color = Color.White)
            }
        }
    }
}
//endregion

//region Smaller Composables/Utility Functions
class TimeLeftShape(
    percentRemaining: Float,
) : Shape {

    private val angle = percentRemaining * 360f
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            var angle = 360f - angle
            if (angle <= 0f) {
                angle = 0.1f
            }
            moveTo(size.width / 2f, size.height / 2f)
            arcTo(Rect(0f, 0f, size.width, size.height), 270f, angle, forceMoveTo = false)
            close()
        }
        return Outline.Generic(path)
    }
}

fun AnnotatedString.Builder.appendGuardianStatusText(guardianStatus: GuardianStatus) {

    val baseStyle =
        SpanStyle(
            fontWeight = FontWeight.W700,
            color = Colors.GreyText
        )

    when (guardianStatus) {
        is GuardianStatus.Accepted ->
            withStyle(baseStyle) {
                append("Awaiting Code")
            }

        is GuardianStatus.Confirmed,
        is GuardianStatus.Onboarded ->
            withStyle(baseStyle.copy(color = Colors.SuccessGreen)) {
                append("Completed")
            }

        GuardianStatus.Declined -> withStyle(baseStyle.copy(color = Color.Red)) {
            append("Declined")
        }

        is GuardianStatus.Invited,
        GuardianStatus.Initial -> withStyle(baseStyle) {
            append("Pending")
        }

        is GuardianStatus.VerificationSubmitted -> withStyle(baseStyle) {
            append("Code Submitted")
        }
    }
}
//endregion

//region Dummy Data
val dummyListOfApprovers = listOf(
    Guardian.ProspectGuardian(
        "Anton",
        participantId = ParticipantId(generatePartitionId().toHexString()),
        invitationId = InvitationId("12345"),
        deviceEncryptedTotpSecret = Base64EncodedData(""),
        status = GuardianStatus.Initial
    ),
    Guardian.ProspectGuardian(
        "Ievgen",
        participantId = ParticipantId(generatePartitionId().toHexString()),
        invitationId = InvitationId("12345"),
        deviceEncryptedTotpSecret = Base64EncodedData(""),
        status = GuardianStatus.Invited(
            invitedAt = Clock.System.now()
        )
    ),
    Guardian.ProspectGuardian(
        "Ata",
        participantId = ParticipantId(generatePartitionId().toHexString()),
        invitationId = InvitationId("12345"),
        deviceEncryptedTotpSecret = Base64EncodedData(""),
        status = GuardianStatus.Accepted(
            acceptedAt = Clock.System.now()
        )
    ),
    Guardian.ProspectGuardian(
        "Ben",
        participantId = ParticipantId(generatePartitionId().toHexString()),
        invitationId = InvitationId("12345"),
        deviceEncryptedTotpSecret = Base64EncodedData(""),
        status = GuardianStatus.VerificationSubmitted(
            signature = Base64EncodedData(""),
            timeMillis = 1L,
            guardianPublicKey = Base58EncodedGuardianPublicKey(""),
            verificationStatus = VerificationStatus.WaitingForVerification,
            submittedAt = Clock.System.now(),
        )
    ),
    Guardian.ProspectGuardian(
        "Brendan",
        participantId = ParticipantId(generatePartitionId().toHexString()),
        invitationId = InvitationId("12345"),
        deviceEncryptedTotpSecret = Base64EncodedData(""),
        status = GuardianStatus.Confirmed(
            guardianKeySignature = Base64EncodedData(""),
            guardianPublicKey = Base58EncodedGuardianPublicKey(""),
            timeMillis = 1L,
            confirmedAt = Clock.System.now()
        )
    ),
    Guardian.ProspectGuardian(
        "Charlie",
        participantId = ParticipantId(generatePartitionId().toHexString()),
        invitationId = InvitationId("12345"),
        deviceEncryptedTotpSecret = Base64EncodedData(""),
        status = GuardianStatus.Declined
    ),
)
//endregion