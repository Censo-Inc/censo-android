package co.censo.vault.presentation.plan_setup.components

import Base58EncodedGuardianPublicKey
import Base64EncodedData
import InvitationId
import LearnMore
import MessageText
import StandardButton
import TitleText
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.data.model.deeplink
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.components.TotpCodeView
import co.censo.vault.R
import co.censo.vault.presentation.VaultColors
import kotlinx.datetime.Clock

@Composable
fun ActivateApproverUI(
    isPrimaryApprover: Boolean = true,
    prospectApprover: Guardian.ProspectGuardian?,
    secondsLeft: Int,
    verificationCode: String,
    storesLink: String, //This should contain both links since approver could be either
    onContinue: () -> Unit,
    onEditNickname: () -> Unit,
) {

    val nickName: String = prospectApprover?.label ?: ""
    val guardianStatus = prospectApprover?.status
    val deeplink = prospectApprover?.deeplink() ?: ""
    val buttonEnabled = prospectApprover?.status is GuardianStatus.Confirmed

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
        verticalArrangement = Arrangement.SpaceBetween,
    ) {

        Spacer(modifier = Modifier.height(24.dp))

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = if (isPrimaryApprover) R.string.activate_primary_approver else R.string.activate_backup_approver,
            textAlign = TextAlign.Start,
        )

        Spacer(modifier = Modifier.height(12.dp))

        MessageText(
            modifier = Modifier.fillMaxWidth(),
            message = R.string.approver_do_3_things,
            textAlign = TextAlign.Start,
        )

        Spacer(modifier = Modifier.height(24.dp))

        ApproverStep(
            imagePainter = painterResource(id = co.censo.shared.R.drawable.share_link),
            heading = stringResource(id = R.string.download_the_app_title),
            content = stringResource(id = R.string.dowloand_the_app_message),
            actionButtonUIData = ActionButtonUIData(
                onClick = { shareLink(storesLink) },
                text = stringResource(R.string.share_app_link),
            ),
        )

        Spacer(modifier = Modifier.height(12.dp))

        ApproverStep(
            imagePainter = painterResource(id = co.censo.shared.R.drawable.share_link),
            heading = stringResource(id = R.string.share_the_link_title),
            content = stringResource(id = R.string.share_the_link_message),
            actionButtonUIData = ActionButtonUIData(
                onClick = { shareLink(deeplink) },
                text = stringResource(R.string.share_unique_link)
            ),
        )

        Spacer(modifier = Modifier.height(12.dp))

        shareTheCodeStepUIData(
            isPrimaryApprover,
            prospectApprover?.status,
            secondsLeft,
            verificationCode,
            context
        )?.let {
            ApproverStep(
                imagePainter = painterResource(id = co.censo.shared.R.drawable.phrase_entry),
                heading = it.heading,
                content = it.content,
                verificationCodeUIData = it.verificationCodeUIData
            )
        }

        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp), color = SharedColors.DividerGray
        )

        Spacer(modifier = Modifier.height(24.dp))

        ProspectApproverInfoBox(
            nickName = nickName,
            primaryApprover = isPrimaryApprover,
            status = guardianStatus,
            onEdit = onEditNickname
        )

        Spacer(modifier = Modifier.height(24.dp))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = buttonEnabled,
            disabledColor = SharedColors.DisabledGrey,
            color = Color.Black,
            contentPadding = PaddingValues(vertical = 12.dp),
            onClick = onContinue
        ) {
            Text(
                fontSize = 20.sp,
                text = stringResource(id = R.string.continue_text),
                color = if (buttonEnabled) Color.White else SharedColors.DisabledFontGrey
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        LearnMore {

        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ProspectApproverInfoBox(
    nickName: String,
    primaryApprover: Boolean,
    status: GuardianStatus?,
    onEdit: () -> Unit
) {

    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent
            )
            .border(
                width = 1.dp,
                color = SharedColors.BorderGrey,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        val labelTextSize = 14.sp

        Column {
            Text(
                text =
                if (primaryApprover) stringResource(R.string.primary_approver)
                else stringResource(R.string.backup_approver),
                color = Color.Black,
                fontSize = labelTextSize
            )

            Text(
                text = nickName,
                color = Color.Black,
                fontSize = 24.sp
            )

            activatedUIData(status, context)?.let {
                Text(
                    text = it.text,
                    color = it.color,
                    fontSize = labelTextSize
                )
            }
        }

        IconButton(onClick = onEdit ) {
            Icon(
                painterResource(id = co.censo.shared.R.drawable.edit_icon),
                contentDescription = stringResource(R.string.edit_approver_name),
                tint = Color.Black
            )
        }

    }

}

fun activatedUIData(guardianStatus: GuardianStatus?, context: Context) =
    when (guardianStatus) {
        is GuardianStatus.Initial,
        is GuardianStatus.Accepted,
        GuardianStatus.Declined -> {
            ApproverActivatedUIData(
                text = context.getString(R.string.not_yet_active),
                color = SharedColors.ErrorRed
            )
        }

        is GuardianStatus.VerificationSubmitted -> {
            ApproverActivatedUIData(
                text = context.getString(R.string.verifying),
                color = SharedColors.ErrorRed
            )
        }

        is GuardianStatus.Confirmed -> {
            ApproverActivatedUIData(
                text = context.getString(R.string.activated),
                color = SharedColors.SuccessGreen
            )
        }

        else -> null
    }

data class ApproverActivatedUIData(
    val text: String,
    val color: Color
)

fun shareTheCodeStepUIData(
    isPrimaryApprover: Boolean,
    status: GuardianStatus?,
    secondsLeft: Int,
    verificationCode: String,
    context: Context
) = when (status) {
        is GuardianStatus.Initial,
        GuardianStatus.Declined -> {
            ShareTheCodeUIData(
                heading = context.getString(R.string.share_the_code_title),
                content = context.getString(R.string.once_they_have_accepted_the_invite_a_unique_confirmation_code_will_be_displayed),
            )
        }

        is GuardianStatus.Accepted,
        is GuardianStatus.VerificationSubmitted -> {
            ShareTheCodeUIData(
                heading = context.getString(R.string.share_the_code_title),
                content = context.getString(R.string.share_the_code_message, if (isPrimaryApprover) context.getString(R.string.primary) else context.getString(R.string.backup)),
                verificationCodeUIData = VerificationCodeUIData(
                    code = verificationCode,
                    timeLeft = secondsLeft
                ),
            )
        }

        is GuardianStatus.Confirmed -> {
            ShareTheCodeUIData(
                heading = context.getString(R.string.share_the_code_title),
                content = context.getString(R.string.the_verification_code_has_been_successfully_confirmed),
            )
        }

        else -> null
    }

data class ShareTheCodeUIData(
    val heading: String,
    val content: String,
    val verificationCodeUIData: VerificationCodeUIData? = null
)

@Composable
fun ApproverStep(
    imagePainter: Painter,
    heading: String,
    content: String,
    verificationCodeUIData: VerificationCodeUIData? = null,
    actionButtonUIData: ActionButtonUIData? = null,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            contentAlignment = Alignment.Center, modifier = Modifier
                .background(
                    color = SharedColors.WordBoxBackground,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Image(
                painter = imagePainter,
                contentDescription = null,
                modifier = Modifier.width(32.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = heading, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = content, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            verificationCodeUIData?.let {
                Box(modifier = Modifier.padding(vertical = 12.dp)) {
                    TotpCodeView(
                        code = it.code,
                        secondsLeft = it.timeLeft,
                        primaryColor = VaultColors.PrimaryColor
                    )
                }
            }

            actionButtonUIData?.let {
                StandardButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = it.onClick
                ) {
                    Text(
                        text = it.text,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

data class ActionButtonUIData(
    val text: String,
    val onClick: () -> Unit
)

data class VerificationCodeUIData(
    val code: String,
    val timeLeft: Int
)

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ActivatePrimaryApproverInitialPreview() {
    ActivateApproverUI(
        isPrimaryApprover = true,
        secondsLeft = 43,
        verificationCode = "345819",
        storesLink = "link",
        prospectApprover = Guardian.ProspectGuardian(
            invitationId = InvitationId(""),
            label = "Neo",
            participantId = ParticipantId.generate(),
            status = GuardianStatus.Initial(
                deviceEncryptedTotpSecret = Base64EncodedData(""),
            )
        ),
        onContinue = {},
        onEditNickname = {}
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ActivatePrimaryApproverAcceptedPreview() {
    ActivateApproverUI(
        isPrimaryApprover = true,
        secondsLeft = 43,
        verificationCode = "345819",
        storesLink = "link",
        prospectApprover = Guardian.ProspectGuardian(
            invitationId = InvitationId(""),
            label = "Neo",
            participantId = ParticipantId.generate(),
            status = GuardianStatus.Accepted(
                deviceEncryptedTotpSecret = Base64EncodedData(""),
                acceptedAt = Clock.System.now()
            )
        ),
        onContinue = {},
        onEditNickname = {}
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ActivatePrimaryApproverConfirmedPreview() {
    ActivateApproverUI(
        isPrimaryApprover = true,
        secondsLeft = 43,
        verificationCode = "345819",
        storesLink = "link",
        prospectApprover = Guardian.ProspectGuardian(
            invitationId = InvitationId(""),
            label = "Neo",
            participantId = ParticipantId.generate(),
            status = GuardianStatus.Confirmed(
                guardianPublicKey = Base58EncodedGuardianPublicKey(""),
                guardianKeySignature = Base64EncodedData(""),
                timeMillis = 0,
                confirmedAt = Clock.System.now()
            )
        ),
        onContinue = {},
        onEditNickname = {}
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ActivateBackupApproverInitialPreview() {
    ActivateApproverUI(
        isPrimaryApprover = false,
        secondsLeft = 43,
        verificationCode = "345819",
        storesLink = "link",
        prospectApprover = Guardian.ProspectGuardian(
            invitationId = InvitationId(""),
            label = "John Wick",
            participantId = ParticipantId.generate(),
            status = GuardianStatus.Initial(
                deviceEncryptedTotpSecret = Base64EncodedData(""),
            )
        ),
        onContinue = {},
        onEditNickname = {}
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ActivateBackupApproverAcceptedPreview() {
    ActivateApproverUI(
        isPrimaryApprover = false,
        secondsLeft = 43,
        verificationCode = "345819",
        storesLink = "link",
        prospectApprover = Guardian.ProspectGuardian(
            invitationId = InvitationId(""),
            label = "John Wick",
            participantId = ParticipantId.generate(),
            status = GuardianStatus.Accepted(
                deviceEncryptedTotpSecret = Base64EncodedData(""),
                acceptedAt = Clock.System.now()
            )
        ),
        onContinue = {},
        onEditNickname = {}
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ActivateBackupApproverConfirmedPreview() {
    ActivateApproverUI(
        isPrimaryApprover = false,
        secondsLeft = 43,
        verificationCode = "345819",
        storesLink = "link",
        prospectApprover = Guardian.ProspectGuardian(
            invitationId = InvitationId(""),
            label = "John Wick",
            participantId = ParticipantId.generate(),
            status = GuardianStatus.Confirmed(
                guardianPublicKey = Base58EncodedGuardianPublicKey(""),
                guardianKeySignature = Base64EncodedData(""),
                timeMillis = 0,
                confirmedAt = Clock.System.now()
            )
        ),
        onContinue = {},
        onEditNickname = {}
    )
}