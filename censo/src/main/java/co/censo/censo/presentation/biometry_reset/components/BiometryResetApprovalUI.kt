package co.censo.censo.presentation.biometry_reset.components

import ParticipantId
import TitleText
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.censo.presentation.components.ApproverStep
import co.censo.censo.presentation.components.ShareTheCodeUIData
import co.censo.censo.presentation.components.VerificationCodeUIData
import co.censo.shared.data.model.AuthResetTotpSecret
import co.censo.shared.data.model.AuthenticationResetApproval
import co.censo.shared.data.model.AuthenticationResetApprovalId
import co.censo.shared.data.model.AuthenticationResetApprovalStatus
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.components.KeepScreenOn

@Composable
fun BiometryResetApprovalUI(
    nickname: String,
    approval: AuthenticationResetApproval,
    verificationCode: String,
    secondsLeft: Int,
) {
    val deeplink = approval.v2Deeplink()

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
        verticalArrangement = Arrangement.Top,
    ) {

        Spacer(modifier = Modifier.height(24.dp))

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = "Biometry reset",
            fontSize = 28.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        ApproverStep(
            heading = listOf(
                stringResource(id = R.string.step_1),
                stringResource(id = R.string.share_this_link_title)
            ),
            content = stringResource(R.string.share_link_access_message, nickname),
            onClick = { shareLink(deeplink) }
        ) {
            Image(
                modifier = Modifier.size(44.dp),
                painter = painterResource(id = R.drawable.censo_login_logo),
                contentDescription = null
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        shareTheCodeStepUIData(
            approval.status,
            secondsLeft,
            verificationCode,
            nickname,
            context
        )?.let {
            ApproverStep(
                heading = it.heading,
                content = it.content,
                verificationCodeUIData = it.verificationCodeUIData,
                includeLine = false
            ) {
                Image(
                    modifier = Modifier.size(44.dp),
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
    }
    KeepScreenOn()
}

fun shareTheCodeStepUIData(
    status: AuthenticationResetApprovalStatus,
    secondsLeft: Int,
    verificationCode: String,
    approverNickname: String,
    context: Context
) = when (status) {

    AuthenticationResetApprovalStatus.Initial,
    AuthenticationResetApprovalStatus.Rejected -> {
        ShareTheCodeUIData(
            heading = listOf(
                context.getString(R.string.step_2),
                context.getString(R.string.read_the_code_title),
            ),
            content = context.getString(
                R.string.onboarding_approver_read_auth_code,
                approverNickname,
            ),
        )
    }

    AuthenticationResetApprovalStatus.WaitingForTotp,
    AuthenticationResetApprovalStatus.TotpVerificationFailed,
    AuthenticationResetApprovalStatus.WaitingForVerification -> {
        ShareTheCodeUIData(
            heading = listOf(
                context.getString(R.string.step_2),
                context.getString(R.string.read_the_code_title),
            ),
            content = context.getString(R.string.share_the_code_message, approverNickname),
            verificationCodeUIData = VerificationCodeUIData(
                code = verificationCode,
                timeLeft = secondsLeft
            ),
        )
    }

    AuthenticationResetApprovalStatus.Approved -> {
        ShareTheCodeUIData(
            heading = listOf(
                context.getString(R.string.step_2),
                context.getString(R.string.read_the_code_title),
            ),
            content = context.getString(R.string.approver_has_approver_your_request, approverNickname),
        )
    }

    else -> null
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun BiometryResetApprovalUIInitialPreview() {
    BiometryResetApprovalUI(
        nickname = "Buddy",
        approval = AuthenticationResetApproval(
            participantId = ParticipantId.generate(),
            status = AuthenticationResetApprovalStatus.Initial,
            guid = AuthenticationResetApprovalId(""),
            totpSecret = AuthResetTotpSecret("")
        ),
        verificationCode = "11111",
        secondsLeft = 38,
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun BiometryResetApprovalUIWaitingForTotpPreview() {
    BiometryResetApprovalUI(
        nickname = "Buddy",
        approval = AuthenticationResetApproval(
            participantId = ParticipantId.generate(),
            status = AuthenticationResetApprovalStatus.WaitingForTotp,
            guid = AuthenticationResetApprovalId(""),
            totpSecret = AuthResetTotpSecret("")
        ),
        verificationCode = "592654",
        secondsLeft = 38,
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun BiometryResetApprovalUIApprovedPreview() {
    BiometryResetApprovalUI(
        nickname = "Buddy",
        approval = AuthenticationResetApproval(
            participantId = ParticipantId.generate(),
            status = AuthenticationResetApprovalStatus.Approved,
            guid = AuthenticationResetApprovalId(""),
            totpSecret = AuthResetTotpSecret("")
        ),
        verificationCode = "592654",
        secondsLeft = 38,
    )
}
