package co.censo.censo.presentation.plan_setup.components

import Base58EncodedGuardianPublicKey
import Base64EncodedData
import InvitationId
import StandardButton
import TitleText
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
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
import co.censo.censo.R
import co.censo.censo.presentation.VaultColors
import kotlinx.datetime.Clock

@Composable
fun ActivateApproverUI(
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
            title = stringResource(R.string.activate_approver_name, nickName),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        ApproverStep(
            imagePainter = painterResource(id = co.censo.shared.R.drawable.share_link),
            step = 1,
            heading = stringResource(id = R.string.share_the_app_title),
            content = stringResource(R.string.share_the_app_link_for_download, nickName, nickName),
            onShareLink = { shareLink(storesLink) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        ApproverStep(
            imagePainter = painterResource(id = co.censo.shared.R.drawable.share_link),
            step = 2,
            heading = stringResource(id = R.string.share_the_link_title),
            content = stringResource(
                R.string.share_invite_link_approver_onboarding,
                nickName,
                nickName
            ),
            onShareLink = { shareLink(deeplink) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        shareTheCodeStepUIData(
            prospectApprover?.status,
            secondsLeft,
            verificationCode,
            nickName,
            context
        )?.let {
            ApproverStep(
                imageVector = Icons.Filled.GraphicEq,
                heading = it.heading,
                content = it.content,
                verificationCodeUIData = it.verificationCodeUIData
            )
            
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

    }
}

@Composable
fun ProspectApproverInfoBox(
    nickName: String,
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
                text = stringResource(id = R.string.approver),
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
        GuardianStatus.Declined -> {
            ApproverActivatedUIData(
                text = context.getString(R.string.not_yet_active),
                color = SharedColors.GreyText
            )
        }

        is GuardianStatus.Accepted -> {
            ApproverActivatedUIData(
                text = context.getString(R.string.opened_link_in_app),
                color = SharedColors.GreyText
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
                text = context.getString(R.string.active),
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
    status: GuardianStatus?,
    secondsLeft: Int,
    verificationCode: String,
    approverNickname: String,
    context: Context
) = when (status) {
        is GuardianStatus.Initial,
        GuardianStatus.Declined -> {
            ShareTheCodeUIData(
                heading = context.getString(R.string.read_the_code_title),
                content = context.getString(
                    R.string.onboarding_approver_read_auth_code,
                    approverNickname,
                    approverNickname
                ),
            )
        }

        is GuardianStatus.Accepted,
        is GuardianStatus.VerificationSubmitted -> {
            ShareTheCodeUIData(
                heading = context.getString(R.string.read_the_code_title),
                content = context.getString(R.string.share_the_code_message, approverNickname),
                verificationCodeUIData = VerificationCodeUIData(
                    code = verificationCode,
                    timeLeft = secondsLeft
                ),
            )
        }

        is GuardianStatus.Confirmed -> {
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
    imagePainter: Painter,
    step: Int? = null,
    heading: String,
    content: String,
    verificationCodeUIData: VerificationCodeUIData? = null,
    onShareLink: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = createIconBoxModifier(addBorder = onShareLink != null, onClick = onShareLink)
        ) {
            Image(
                painter = imagePainter,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(2.dp),
            )

            step?.let {
                Text(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 3.dp, top = 2.dp),
                    text = it.toString(), fontSize = 10.sp, color = Color.Black
                )
            }
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
        }
    }
}

@Composable
fun ApproverStep(
    imagePainter: Painter,
    step: Int? = null,
    heading: String,
    content: AnnotatedString,
    stringAnnotationTag: String? = null,
    verificationCodeUIData: VerificationCodeUIData? = null,
    onShareLink: (() -> Unit)? = null,
) {
    val uriHandler = LocalUriHandler.current

    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = createIconBoxModifier(addBorder = onShareLink != null, onClick = onShareLink)
        ) {
            Image(
                painter = imagePainter,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(2.dp),
            )

            step?.let {
                Text(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 3.dp, top = 2.dp),
                    text = it.toString(), fontSize = 10.sp, color = Color.Black
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = heading, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            ClickableText(text = content, style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)) { offset ->
                content.getStringAnnotations(stringAnnotationTag ?: "", start = offset, end = offset).firstOrNull()
                    ?.let { annotatedLink ->
                        uriHandler.openUri(annotatedLink.item)
                    }
            }
            Spacer(modifier = Modifier.height(12.dp))
            verificationCodeUIData?.let {
                Box(modifier = Modifier.padding(vertical = 12.dp)) {
                    TotpCodeView(
                        code = it.code,
                        secondsLeft = it.timeLeft,
                        primaryColor = VaultColors.PrimaryColor
                    )
                }
            }
        }
    }
}

@Composable
fun ApproverStep(
    imageVector: ImageVector,
    step: Int? = null,
    heading: String,
    content: String,
    verificationCodeUIData: VerificationCodeUIData? = null,
    onShareLink: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = createIconBoxModifier(addBorder = onShareLink != null, onClick = onShareLink)
        ) {
            Image(
                imageVector = imageVector,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
                    .padding(2.dp),
            )

            step?.let {
                Text(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 3.dp, top = 2.dp),
                    text = it.toString(), fontSize = 10.sp, color = Color.Black
                )
            }
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
        }
    }
}

fun createIconBoxModifier(addBorder: Boolean, onClick: (() -> Unit)? = null): Modifier =
    if (addBorder) Modifier
        .size(48.dp)
        .clickable { onClick?.invoke() }
        .border(width = 1.dp, color = Color.Black, shape = RoundedCornerShape(8.dp))
        .background(
            color = SharedColors.WordBoxBackground,
            shape = RoundedCornerShape(8.dp)
        )
    else Modifier
        .size(48.dp)
        .background(
            color = SharedColors.WordBoxBackground,
            shape = RoundedCornerShape(8.dp)
        )

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
fun ActivateAlternateApproverInitialPreview() {
    ActivateApproverUI(
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
fun ActivateAlternateApproverAcceptedPreview() {
    ActivateApproverUI(
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
fun ActivateAlternateApproverConfirmedPreview() {
    ActivateApproverUI(
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