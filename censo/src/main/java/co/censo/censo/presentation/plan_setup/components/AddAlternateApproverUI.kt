package co.censo.censo.presentation.plan_setup.components

import MessageText
import StandardButton
import TitleText
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
import co.censo.censo.R
import co.censo.shared.presentation.ButtonTextStyle

@Composable
fun AddAlternateApproverUI(
    onInviteAlternateSelected: () -> Unit
) {

    val verticalSpacingHeight = 28.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = R.string.add_an_alternate_approver,
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        val spannedMessageText = buildSpannedText(
            preceding = stringResource(id = R.string.add_second_approver_message_first_span),
            remaining = stringResource(id = R.string.add_second_approver_message_second_span),
            textToApplySpanStyle = stringResource(id = R.string.second_approver_spanned_text)
        )

        MessageText(
            modifier = Modifier.fillMaxWidth(),
            message = spannedMessageText,
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight + 24.dp))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onInviteAlternateSelected,
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.invite_alternate),
                style = ButtonTextStyle.copy(fontSize = 24.sp)
            )
        }

        Spacer(modifier = Modifier.height(verticalSpacingHeight))
    }
}

@Preview(showBackground = true)
@Composable
fun AddAlternateApproverUIPreview() {
    AddAlternateApproverUI(
        onInviteAlternateSelected = {},
    )
}

//TODO: Convert this into a util/helper method and centralize all usage
fun buildSpannedText(
    preceding: String,
    remaining: String,
    textToApplySpanStyle: String
): AnnotatedString {
    val boldSpanStyle = SpanStyle(
        fontWeight = FontWeight.W700
    )

    return buildAnnotatedString {
        append("$preceding ")
        withStyle(boldSpanStyle) {
            append(textToApplySpanStyle)
        }
        append(" $remaining")
    }
}