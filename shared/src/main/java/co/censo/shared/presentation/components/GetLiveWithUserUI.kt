package co.censo.shared.presentation.components

import LearnMore
import MessageText
import StandardButton
import TitleText
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GetLiveWithUserUI(
    title: String,
    message: String,
    showSecondButton: Boolean = true,
    onContinueLive: () -> Unit,
    onResumeLater: () -> Unit,
) {

    val verticalSpacingHeight = 28.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {

        TitleText(
            modifier = Modifier.fillMaxWidth(),
            title = title,
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(verticalSpacingHeight))

        MessageText(
            modifier = Modifier.fillMaxWidth(),
            message = message,
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

        if (showSecondButton) {
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
        }

        LearnMore {

        }

        Spacer(modifier = Modifier.height(verticalSpacingHeight))
    }
}

@Preview(showBackground = true)
@Composable
fun GetLiveWithUserUIPreview() {
    GetLiveWithUserUI(
        title = "Get live with Neo",
        message = "For maximum security yada yada and then go do this thing over there and think about all sorts of things.",
        onContinueLive = {},
        onResumeLater = {},
    )
}