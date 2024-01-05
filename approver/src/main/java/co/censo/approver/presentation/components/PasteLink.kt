package co.censo.approver.presentation.components

import MessageText
import StandardButton
import TitleText
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.approver.R
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors
import co.censo.approver.R as ApproverR

@Composable
fun PasteLink(
    isApprover: Boolean,
    onPasteLinkClick: () -> Unit,
    onAssistClick: () -> Unit
) {
    if (isApprover) {
        PasteOrAssist(
            onPasteLinkClick = onPasteLinkClick,
            onAssistClick = onAssistClick
        )
    } else {
        Paste(
            onPasteLinkClick = onPasteLinkClick
        )
    }
}

@Composable
private fun PasteOrAssist(onPasteLinkClick: () -> Unit, onAssistClick: () -> Unit) {
    Column(
        Modifier
            .background(color = Color.White)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        val verticalSpacingBetweenItems = 24.dp

        Spacer(modifier = Modifier.weight(0.25f))

        TitleText(
            title = stringResource(R.string.received_a_link),
        )

        Spacer(modifier = Modifier.height(verticalSpacingBetweenItems))

        MessageText(
            message = stringResource(R.string.if_the_person_you_are_assisting_has_set_you_a_link_you_can_tap_on_it_to_continue),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(verticalSpacingBetweenItems))

        MessageText(
            message = ApproverR.string.or_simply_copy_link,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(verticalSpacingBetweenItems))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp),
            onClick = onPasteLinkClick,
        ) {
            Text(
                text = stringResource(ApproverR.string.paste_link),
                style = ButtonTextStyle.copy(fontSize = 22.sp)
            )
        }

        Spacer(modifier = Modifier.weight(0.25f))

        Divider()

        Spacer(modifier = Modifier.weight(0.25f))

        TitleText(
            title = stringResource(R.string.asked_for_login_assistance),
        )

        Spacer(modifier = Modifier.height(verticalSpacingBetweenItems))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp),
            onClick = onAssistClick,
        ) {
            Text(
                text = stringResource(R.string.assist),
                style = ButtonTextStyle.copy(fontSize = 22.sp)
            )
        }

        Spacer(modifier = Modifier.weight(0.25f))
    }
}

@Composable
private fun Paste(onPasteLinkClick: () -> Unit) {
    Column(
        Modifier
            .background(color = Color.White)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        val verticalSpacingBetweenItems = 24.dp

        Spacer(modifier = Modifier.weight(0.5f))

        Image(
            painter = painterResource(id = ApproverR.drawable.main_export_link),
            contentDescription = null,
            colorFilter = ColorFilter.tint(color = SharedColors.MainIconColor)
        )

        Spacer(modifier = Modifier.height(verticalSpacingBetweenItems))

        MessageText(
            message = ApproverR.string.get_unique_link,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(verticalSpacingBetweenItems))

        MessageText(
            message = ApproverR.string.once_receive_it,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(verticalSpacingBetweenItems))

        MessageText(
            message = ApproverR.string.or_simply_copy_link,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(verticalSpacingBetweenItems))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp),
            onClick = onPasteLinkClick,
        ) {
            Text(
                text = stringResource(ApproverR.string.paste_link),
                style = ButtonTextStyle.copy(fontSize = 22.sp)
            )
        }

        Spacer(modifier = Modifier.weight(0.5f))
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PasteLinkIsApproverPreview() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        PasteLink(
            isApprover = true,
            onPasteLinkClick = {},
            onAssistClick = {}
        )
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PasteLinkIsNotApproverPreview() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        PasteLink(
            isApprover = false,
            onPasteLinkClick = {},
            onAssistClick = {}
        )
    }
}