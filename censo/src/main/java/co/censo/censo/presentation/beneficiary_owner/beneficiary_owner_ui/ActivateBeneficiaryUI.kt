package co.censo.censo.presentation.beneficiary_owner.beneficiary_owner_ui

import Base64EncodedData
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.censo.censo.R
import co.censo.censo.presentation.components.ApproverStep
import co.censo.censo.presentation.components.ShareTheCodeUIData
import co.censo.censo.presentation.components.VerificationCodeUIData
import co.censo.censo.presentation.plan_setup.components.ProspectApproverInfoBox
import co.censo.shared.data.model.ApproverStatus
import co.censo.shared.data.model.Beneficiary
import co.censo.shared.data.model.BeneficiaryInvitationId
import co.censo.shared.data.model.BeneficiaryStatus
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.DisabledButtonTextStyle
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.components.KeepScreenOn

@Composable
fun ActivateBeneficiaryUI(
    beneficiary: Beneficiary,
    deeplink: String?,
    secondsLeft: Int,
    verificationCode: String,
    storesLink: String,
    onContinue: () -> Unit,
) {
    val nickName: String = beneficiary.label ?: ""
    val context = LocalContext.current
    val buttonEnabled = beneficiary.status is BeneficiaryStatus.Activated

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
                stringResource(id = R.string.share_censo_app_title)
            ),
            content = stringResource(R.string.share_the_censo_app_link_for_download, nickName),
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
                R.string.share_invite_link_beneficiary_onboarding,
                nickName,
            ),
            buttonText = stringResource(id = R.string.invite),
            onClick = {
                deeplink?.let { shareLink(it) }
            }
        ) {
            Image(
                modifier = Modifier.size(66.dp),
                painter = painterResource(id = co.censo.shared.R.drawable.approver_icon),
                contentDescription = null
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        shareTheCodeStepUIData(
            beneficiary.status,
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

        ProspectBeneficiaryInfoBox(
            nickName = nickName,
            status = beneficiary.status,
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

fun shareTheCodeStepUIData(
    status: BeneficiaryStatus?,
    secondsLeft: Int,
    verificationCode: String,
    beneficiaryNickname: String,
    context: Context
) = when (status) {
    is BeneficiaryStatus.Initial -> {
        ShareTheCodeUIData(
            heading = listOf(
                context.getString(R.string.step_3),
                context.getString(R.string.read_the_code_title),
            ),
            content = context.getString(
                R.string.onboarding_beneficiary_read_auth_code,
                beneficiaryNickname,
            ),
        )
    }

    is BeneficiaryStatus.Accepted,
    is BeneficiaryStatus.VerificationSubmitted -> {
        ShareTheCodeUIData(
            heading = listOf(
                context.getString(R.string.step_3),
                context.getString(R.string.read_the_code_title),
            ),
            content = context.getString(
                R.string.share_the_beneficiary_code_message,
                beneficiaryNickname
            ),
            verificationCodeUIData = VerificationCodeUIData(
                code = verificationCode,
                timeLeft = secondsLeft
            ),
        )
    }

    is BeneficiaryStatus.Activated -> {
        ShareTheCodeUIData(
            heading = listOf(
                context.getString(R.string.step_3),
                context.getString(R.string.read_the_code_title),
            ),
            content = context.getString(R.string.approver_is_activated, beneficiaryNickname),
        )
    }

    else -> null
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ActivateBeneficiaryUI() {
    ActivateBeneficiaryUI(
        beneficiary = Beneficiary(
            label = "Ben Eficiary",
            status = BeneficiaryStatus.Initial(
                invitationId = BeneficiaryInvitationId(""),
                deviceEncryptedTotpSecret = Base64EncodedData("")
            )
        ),
        deeplink = "",
        secondsLeft = 45,
        verificationCode = "345182",
        storesLink = ""
    ) {

    }
}
