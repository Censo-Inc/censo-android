package co.censo.approver.presentation.reset_links.components

import MessageText
import StandardButton
import TitleText
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.approver.R
import co.censo.shared.presentation.ButtonTextStyle

@Composable
fun GetLiveWithOwnerUI(
    onContinue: () -> Unit,
) {
    val verticalSpacingHeight = 28.dp

    Box(modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 36.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            TitleText(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.get_live_with_owner_title),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(verticalSpacingHeight - 8.dp))

            MessageText(
                modifier = Modifier.fillMaxWidth(),
                message = stringResource(R.string.get_live_with_owner_message),
                textAlign = TextAlign.Start
            )
        }

        StandardButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 32.dp, bottom = 32.dp)
                .align(Alignment.BottomCenter),
            onClick = onContinue,
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            Text(
                text = stringResource(id = co.censo.approver.R.string.continue_text),
                style = ButtonTextStyle.copy(fontSize = 22.sp)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GetLiveWithOwnerUIPreview() {
    Box(modifier = Modifier.background(Color.White)) {
        GetLiveWithOwnerUI(
            onContinue = {}
        )
    }
}