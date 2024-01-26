package co.censo.censo.presentation.plan_setup.components

import Base58EncodedApproverPublicKey
import Base64EncodedData
import InvitationId
import ParticipantId
import StandardButton
import TitleText
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.censo.censo.R
import co.censo.censo.presentation.components.ApproverStep
import co.censo.censo.presentation.components.ShareTheCodeUIData
import co.censo.censo.presentation.components.VerificationCodeUIData
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.ApproverStatus
import co.censo.shared.data.model.deeplink
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.DisabledButtonTextStyle
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.components.KeepScreenOn
import kotlinx.datetime.Clock

@Composable
fun ActivateApproverUI(
    prospectApprover: Approver.ProspectApprover?,
    secondsLeft: Int,
    verificationCode: String,
    storesLink: String, //This should contain both links since approver could be either
    onContinue: () -> Unit,
    onEditNickname: () -> Unit,
) {

    val nickName: String = prospectApprover?.label ?: ""
    val approverStatus = prospectApprover?.status
    val deeplink = prospectApprover?.deeplink() ?: ""
    val buttonEnabled = prospectApprover?.status is ApproverStatus.Confirmed

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
            title = stringResource(R.string.activate_approver_name, nickName),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        ApproverStep(
            heading = listOf(
                stringResource(id = R.string.step_1),
                stringResource(id = R.string.share_the_app_title)
            ),
            content = stringResource(R.string.share_the_app_link_for_download, nickName),
            onClick = { shareLink(storesLink) }
        ) {
            Image(
                modifier = Modifier.size(66.dp),
                painter = painterResource(id = R.drawable.censo_login_logo),
                contentDescription = null
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ApproverStep(
            heading = listOf(
                stringResource(id = R.string.step_2),
                stringResource(id = R.string.share_the_link_title),
            ),
            content = stringResource(
                R.string.share_invite_link_approver_onboarding,
                nickName,
            ),
            buttonText = stringResource(id = R.string.invite),
            onClick = { shareLink(deeplink) }
        ) {
            Image(
                modifier = Modifier.size(66.dp),
                painter = painterResource(id = co.censo.shared.R.drawable.approver_icon),
                contentDescription = null
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        shareTheCodeStepUIData(
            prospectApprover?.status,
            secondsLeft,
            verificationCode,
            nickName,
            context
        )?.let {
            ApproverStep(
                heading = it.heading,
                content = it.content,
                verificationCodeUIData = it.verificationCodeUIData,
                includeLine = false
            ) {
                Image(
                    modifier = Modifier.size(66.dp),
                    painter = painterResource(id = co.censo.shared.R.drawable.phonewaveform),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(color = SharedColors.MainIconColor)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp), color = SharedColors.DividerGray
        )

        Spacer(modifier = Modifier.height(24.dp))

        ProspectApproverInfoBox(
            nickName = nickName,
            status = approverStatus,
            onEdit = onEditNickname
        )

        Spacer(modifier = Modifier.height(24.dp))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = buttonEnabled,
            contentPadding = PaddingValues(vertical = 12.dp),
            onClick = onContinue
        ) {
            val continueButtonTextStyle =
                if (buttonEnabled) ButtonTextStyle else DisabledButtonTextStyle

            Text(
                text = stringResource(id = R.string.continue_text),
                style = continueButtonTextStyle
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

    }
    KeepScreenOn()
}

fun activatedUIData(approverStatus: ApproverStatus?, context: Context) =
    when (approverStatus) {
        is ApproverStatus.Initial,
        ApproverStatus.Declined -> {
            ApproverActivatedUIData(
                text = context.getString(R.string.not_yet_active),
                color = SharedColors.GreyText
            )
        }

        is ApproverStatus.Accepted -> {
            ApproverActivatedUIData(
                text = context.getString(R.string.opened_link_in_app),
                color = SharedColors.GreyText
            )
        }

        is ApproverStatus.VerificationSubmitted -> {
            ApproverActivatedUIData(
                text = context.getString(R.string.verifying),
                color = SharedColors.ErrorRed
            )
        }

        is ApproverStatus.Confirmed -> {
            ApproverActivatedUIData(
                text = context.getString(R.string.verified),
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
    status: ApproverStatus?,
    secondsLeft: Int,
    verificationCode: String,
    approverNickname: String,
    context: Context
) = when (status) {
    is ApproverStatus.Initial,
    ApproverStatus.Declined -> {
        ShareTheCodeUIData(
            heading = listOf(
                context.getString(R.string.step_3),
                context.getString(R.string.read_the_code_title),
            ),
            content = context.getString(
                R.string.onboarding_approver_read_auth_code,
                approverNickname,
            ),
        )
    }

    is ApproverStatus.Accepted,
    is ApproverStatus.VerificationSubmitted -> {
        ShareTheCodeUIData(
            heading = listOf(
                context.getString(R.string.step_3),
                context.getString(R.string.read_the_code_title),
            ),
            content = context.getString(R.string.share_the_code_message, approverNickname),
            verificationCodeUIData = VerificationCodeUIData(
                code = verificationCode,
                timeLeft = secondsLeft
            ),
        )
    }

    is ApproverStatus.Confirmed -> {
        ShareTheCodeUIData(
            heading = listOf(
                context.getString(R.string.step_3),
                context.getString(R.string.read_the_code_title),
            ),
            content = context.getString(R.string.approver_is_activated, approverNickname),
        )
    }

    else -> null
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ActivatePrimaryApproverInitialPreview() {
    ActivateApproverUI(
        secondsLeft = 43,
        verificationCode = "345819",
        storesLink = "link",
        prospectApprover = Approver.ProspectApprover(
            invitationId = InvitationId(""),
            label = "Neo",
            participantId = ParticipantId.generate(),
            status = ApproverStatus.Initial(
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
        secondsLeft = 43,
        verificationCode = "345819",
        storesLink = "link",
        prospectApprover = Approver.ProspectApprover(
            invitationId = InvitationId(""),
            label = "Neo",
            participantId = ParticipantId.generate(),
            status = ApproverStatus.Accepted(
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
        secondsLeft = 43,
        verificationCode = "345819",
        storesLink = "link",
        prospectApprover = Approver.ProspectApprover(
            invitationId = InvitationId(""),
            label = "Neo",
            participantId = ParticipantId.generate(),
            status = ApproverStatus.Confirmed(
                approverPublicKey = Base58EncodedApproverPublicKey(""),
                approverKeySignature = Base64EncodedData(""),
                timeMillis = 0,
                confirmedAt = Clock.System.now()
            )
        ),
        onContinue = {},
        onEditNickname = {}
    )
}

@Preview(device = Devices.NEXUS_5, showSystemUi = true, showBackground = true)
@Composable
fun ActivateAlternateApproverInitialPreview() {
    ActivateApproverUI(
        secondsLeft = 43,
        verificationCode = "345819",
        storesLink = "link",
        prospectApprover = Approver.ProspectApprover(
            invitationId = InvitationId(""),
            label = "John Wick has a really nice car",
            participantId = ParticipantId.generate(),
            status = ApproverStatus.Initial(
                deviceEncryptedTotpSecret = Base64EncodedData(""),
            )
        ),
        onContinue = {},
        onEditNickname = {}
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ActivateAlternateApproverAcceptedPreview() {
    ActivateApproverUI(
        secondsLeft = 43,
        verificationCode = "345819",
        storesLink = "link",
        prospectApprover = Approver.ProspectApprover(
            invitationId = InvitationId(""),
            label = "John Wick",
            participantId = ParticipantId.generate(),
            status = ApproverStatus.Accepted(
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
fun ActivateAlternateApproverConfirmedPreview() {
    ActivateApproverUI(
        secondsLeft = 43,
        verificationCode = "345819",
        storesLink = "link",
        prospectApprover = Approver.ProspectApprover(
            invitationId = InvitationId(""),
            label = "John Wick",
            participantId = ParticipantId.generate(),
            status = ApproverStatus.Confirmed(
                approverPublicKey = Base58EncodedApproverPublicKey(""),
                approverKeySignature = Base64EncodedData(""),
                timeMillis = 0,
                confirmedAt = Clock.System.now()
            )
        ),
        onContinue = {},
        onEditNickname = {}
    )
}