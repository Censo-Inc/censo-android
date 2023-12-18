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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@Composable
fun SetupApproversScreen(
    approverSetupExists: Boolean,
    onInviteApproversSelected: () -> Unit,
    onCancelApproverOnboarding: () -> Unit,
) {
    val verticalSpacingHeight = 24.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .padding(horizontal = 36.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.you_can_increase_your_security),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight + 12.dp))

        MessageText(
            message = buildSpannedParagraph(
                preceding = stringResource(R.string.adding_approvers_makes_you_more_secure_span),
                bolded = stringResource(R.string.require),
                remaining = stringResource(R.string.an_approval_from_one_of_your_approvers),
            ),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        MessageText(
            message = stringResource(R.string.adding_approvers_ensures_span),
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
            TextButton(
                onClick = onCancelApproverOnboarding,
                modifier = Modifier.padding(end = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = ButtonTextStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.W400, color = SharedColors.GreyText)
                )
            }
        }

        Spacer(modifier = Modifier.height(verticalSpacingHeight + 24.dp))
    }
}

private fun buildSpannedParagraph(
    preceding: String,
    bolded: String,
    remaining: String
): AnnotatedString {
    val boldSpanStyle = SpanStyle(
        fontWeight = FontWeight.W700
    )

    return buildAnnotatedString {
        append("$preceding ")
        withStyle(boldSpanStyle) {
            append(bolded)
        }
        append(" $remaining")
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewSetupExistsApproversHome() {
    SetupApproversScreen(
        approverSetupExists = true,
        onInviteApproversSelected = {},
        onCancelApproverOnboarding = {},
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewNoSetupExistsApproverHome() {
    SetupApproversScreen(
        approverSetupExists = false,
        onInviteApproversSelected = {},
        onCancelApproverOnboarding = {},
    )
}