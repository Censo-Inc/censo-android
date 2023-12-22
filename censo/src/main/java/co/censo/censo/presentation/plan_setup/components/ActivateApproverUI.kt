package co.censo.censo.presentation.plan_setup.components

import Base58EncodedApproverPublicKey
import Base64EncodedData
import InvitationId
import StandardButton
import TitleText
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.ApproverStatus
import co.censo.shared.data.model.deeplink
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.components.TotpCodeView
import co.censo.censo.R
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.DisabledButtonTextStyle
import co.censo.shared.presentation.SharedColors.ButtonBackgroundBlue
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
            heading = stringResource(id = R.string.share_the_app_title),
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
            heading = stringResource(id = R.string.share_the_link_title),
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
            heading = context.getString(R.string.read_the_code_title),
            content = context.getString(
                R.string.onboarding_approver_read_auth_code,
                approverNickname,
            ),
        )
    }

    is ApproverStatus.Accepted,
    is ApproverStatus.VerificationSubmitted -> {
        ShareTheCodeUIData(
            heading = context.getString(R.string.read_the_code_title),
            content = context.getString(R.string.share_the_code_message, approverNickname),
            verificationCodeUIData = VerificationCodeUIData(
                code = verificationCode,
                timeLeft = secondsLeft
            ),
        )
    }

    is ApproverStatus.Confirmed -> {
        ShareTheCodeUIData(
            heading = context.getString(R.string.read_the_code_title),
            content = context.getString(R.string.approver_is_activated, approverNickname),
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
    heading: String,
    content: String,
    buttonText: String = stringResource(id = R.string.share),
    verificationCodeUIData: VerificationCodeUIData? = null,
    onClick: (() -> Unit)? = null,
    includeLine: Boolean = true,
    imageContent: @Composable() ((ColumnScope.() -> Unit))
) {
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            imageContent()
            if (includeLine) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(3.dp)
                        .background(color = ButtonBackgroundBlue)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.Start,
        ) {
            StepTitleText(heading = heading)
            Spacer(modifier = Modifier.height(8.dp))
            StepContentText(content = content)
            Spacer(modifier = Modifier.height(8.dp))
            onClick?.let {
                StepButton(
                    icon = painterResource(id = co.censo.shared.R.drawable.share_link),
                    text = buttonText,
                    onClick = it
                )
            }
            verificationCodeUIData?.let {
                Box(modifier = Modifier.padding(vertical = 12.dp)) {
                    TotpCodeView(
                        code = it.code,
                        secondsLeft = it.timeLeft,
                        primaryColor = SharedColors.MainColorText
                    )
                }
            }
        }
    }
}

@Composable
fun StepButton(icon: Painter, text: String, onClick: () -> Unit) {
    StandardButton(
        onClick = onClick,
        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = icon,
                contentDescription = null,
                tint = SharedColors.ButtonTextBlue
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = ButtonTextStyle.copy(fontSize = 20.sp, fontWeight = null)
            )
        }
    }
}

@Composable
fun StepTitleText(heading: String) {
    Text(
        text = heading,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = SharedColors.MainColorText
    )
}

@Composable
fun StepContentText(content: String) {
    Text(
        text = content,
        fontSize = 18.sp,
        color = SharedColors.MainColorText
    )
}

data class VerificationCodeUIData(
    val code: String,
    val timeLeft: Int
)

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