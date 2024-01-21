package co.censo.censo.presentation.main

import MessageText
import StandardButton
import TitleText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.presentation.SharedColors
import co.censo.censo.R
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.components.LearnMoreScreen
import co.censo.shared.presentation.components.LearnMoreUI
import co.censo.shared.presentation.components.LearnMoreUtil

@Composable
fun SetupApproversScreen(
    approverSetupExists: Boolean,
    isInfoViewVisible: Boolean,
    onInviteApproversSelected: () -> Unit,
    onCancelApproverOnboarding: () -> Unit,
    onShowInfoView: () -> Unit
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val verticalSpacingHeight = screenHeight * 0.025f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .padding(horizontal = 36.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.add_approvers_security_title),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight + 12.dp))

        MessageText(
            message = stringResource(R.string.adding_approvers_makes_you_more_secure_span),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onInviteApproversSelected,
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.approvers),
                    contentDescription = null,
                    tint = SharedColors.ButtonTextBlue
                )
                Spacer(modifier = Modifier.width(12.dp))
                if (approverSetupExists) {
                    Text(
                        text = stringResource(R.string.resume_adding_approvers_button_text),
                        style = ButtonTextStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.W400)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.add_approvers_button_text),
                        style = ButtonTextStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.W400)
                    )
                }
            }
        }

        if (approverSetupExists) {
            Spacer(modifier = Modifier.height(verticalSpacingHeight))
            TextButton(
                onClick = onCancelApproverOnboarding,
                modifier = Modifier.padding(end = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = ButtonTextStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.W400, color = SharedColors.GreyText)
                )
            }
            Spacer(modifier = Modifier.height(verticalSpacingHeight * 1.5f))
        }

        Spacer(modifier = Modifier.height(verticalSpacingHeight * 2.0f))

        LearnMoreUI {
            onShowInfoView()
        }

        Spacer(modifier = Modifier.height(verticalSpacingHeight))
    }

    if (isInfoViewVisible) {
        LearnMoreScreen(
            title = stringResource(R.string.trusted_approvers_learn_more_title),
            annotatedString = LearnMoreUtil.trustedApproversMessage(),
        )
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewSetupExistsApproversHome() {
    SetupApproversScreen(
        approverSetupExists = true,
        isInfoViewVisible = false,
        onInviteApproversSelected = {},
        onCancelApproverOnboarding = {},
        onShowInfoView = {}
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewNoSetupExistsApproverHome() {
    SetupApproversScreen(
        approverSetupExists = false,
        isInfoViewVisible = false,
        onInviteApproversSelected = {},
        onCancelApproverOnboarding = {},
        onShowInfoView = {}
    )
}