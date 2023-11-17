package co.censo.approver.presentation.components

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
import co.censo.shared.R

@Composable
fun ApproveRequest(
    onContinue: () -> Unit,
) {
    val verticalSpacingHeight = 28.dp

    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 36.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            TitleText(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(co.censo.approver.R.string.approve_request),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(verticalSpacingHeight - 8.dp))

            MessageText(
                modifier = Modifier.fillMaxWidth(),
                message = stringResource(co.censo.approver.R.string.approving_request_message),
                textAlign = TextAlign.Start
            )
        }

        Box(
            modifier = Modifier.fillMaxSize().padding(vertical = 24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            StandardButton(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black,
                onClick = onContinue,
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 32.dp)
            ) {
                Text(
                    text = stringResource(id = co.censo.approver.R.string.continue_text),
                    color = Color.White,
                    fontSize = 24.sp
                )
            }
        }
    }
}

@Preview
@Composable
fun ApproveRequestPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        ApproveRequest { }
    }
}