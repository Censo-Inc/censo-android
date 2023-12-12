package co.censo.shared.presentation.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.R
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors

@Composable
fun GetLiveWithUserUI(
    title: String,
    message: String,
    showSecondButton: Boolean = true,
    activatingApprover: Boolean = false,
    onContinueLive: () -> Unit,
    onResumeLater: () -> Unit,
) {

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    val verticalSpacingHeight = screenHeight * 0.030f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom

    ) {

        Image(
            modifier = Modifier.weight(0.1f).fillMaxWidth(),
            painter = painterResource(id = R.drawable.activateapprover),
            contentDescription = null,
            contentScale = ContentScale.Fit,
        )

        Column(
            modifier = Modifier.padding(horizontal = screenWidth * 0.08f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {

            Spacer(modifier = Modifier.height(screenHeight * 0.05f))

            TitleText(
                modifier = Modifier.fillMaxWidth(),
                title = title,
                color = SharedColors.MainColorText,
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(verticalSpacingHeight - 8.dp))

            MessageText(
                modifier = Modifier.fillMaxWidth(),
                message = message,
                color = SharedColors.MainColorText,
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(verticalSpacingHeight))

            val buttonText =
                if (activatingApprover) stringResource(R.string.activate_now) else stringResource(
                    R.string.continue_live
                )
            StandardButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onContinueLive,
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 32.dp)
            ) {
                Text(
                    text = buttonText,
                    style = ButtonTextStyle.copy(fontSize = 24.sp),
                )
            }

            Spacer(modifier = Modifier.height(verticalSpacingHeight - 12.dp))

            if (showSecondButton) {
                StandardButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onResumeLater,
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 32.dp)
                ) {
                    Text(
                        text = stringResource(R.string.resume_later),
                        style = ButtonTextStyle.copy(fontSize = 24.sp),
                    )
                }

                Spacer(modifier = Modifier.height(verticalSpacingHeight * 2))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GetLiveWithUserUIPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        GetLiveWithUserUI(
            title = "Activate Neo",
            message = "For maximum security yada yada and then go do this thing over there and think about all sorts of things. For maximum security yada yada and then go do this thing over there and think about all sorts of things. For maximum security yada yada and then go do this thing over there and think about all sorts of things. For maximum security yada yada and then go do this thing over there and think about all sorts of things. For maximum security yada yada and then go do this thing over there and think about all sorts of things. For maximum security yada yada and then go do this thing over there and think about all sorts of things. For maximum security yada yada and then go do this thing over there and think about all sorts of things. For maximum security yada yada and then go do this thing over there and think about all sorts of things.",
            activatingApprover = true,
            onContinueLive = {},
            onResumeLater = {},
        )
    }
}