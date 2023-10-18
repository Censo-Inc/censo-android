package co.censo.vault.presentation.plan_setup.components

import LearnMore
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.vault.R

@Composable
fun GetLiveWithApproverUI(
    nickname: String,
    onContinueLive: () -> Unit,
    onResumeLater: () -> Unit
) {

    val verticalSpacingHeight = 28.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {

        // FIXME add nickname
        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = R.string.get_live_with_your_approver,
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        MessageText(
            modifier = Modifier.fillMaxWidth(),
            message = R.string.get_live_with_your_approver_message,
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight + 24.dp))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Black,
            onClick = onContinueLive,
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 32.dp)
        ) {
            Text(
                text = "Continue live",
                color = Color.White,
                fontSize = 24.sp
            )
        }

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Black,
            onClick = onResumeLater,
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 32.dp)
        ) {
            Text(
                text = "Resume later",
                color = Color.White,
                fontSize = 24.sp
            )
        }

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        LearnMore {

        }

        Spacer(modifier = Modifier.height(verticalSpacingHeight))
    }
}

@Preview(showBackground = true)
@Composable
fun GetLiveWithApproverUIPreview() {
    GetLiveWithApproverUI(
        nickname = "Neo",
        onContinueLive = {},
        onResumeLater = {},
    )
}