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
import androidx.compose.ui.tooling.preview.Devices
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
    buttonText: String,
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
            modifier = Modifier.weight(0.1f).fillMaxWidth().padding(top = screenHeight * 0.015f),
            painter = painterResource(id = R.drawable.activate_approver),
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

            StandardButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onContinueLive,
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 32.dp)
            ) {
                Text(
                    text = buttonText,
                    style = ButtonTextStyle.copy(fontSize = 20.sp),
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
                        style = ButtonTextStyle.copy(fontSize = 20.sp),
                    )
                }

                Spacer(modifier = Modifier.height(verticalSpacingHeight * 2))
            }
        }
    }
}

@Preview(device = Devices.PIXEL_4_XL, showSystemUi = true, showBackground = true)
@Composable
fun LargeGetLiveWithUserUIPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        GetLiveWithUserUI(
            title = "Activate Neo",
            message = "Activating New as an approver will take about 2 minutes. This activation should preferable take place while you're on the phone or in-person to ensure you are activating the proper approver.",
            buttonText = "Activate Now",
            onContinueLive = {},
            onResumeLater = {},
        )
    }
}

@Preview(device = Devices.PIXEL_4, showSystemUi = true, showBackground = true)
@Composable
fun NormalGetLiveWithUserUIPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        GetLiveWithUserUI(
            title = "Activate Neo",
            message = "Activating New as an approver will take about 2 minutes. This activation should preferable take place while you're on the phone or in-person to ensure you are activating the proper approver.",
            buttonText = "Verify Now",
            onContinueLive = {},
            onResumeLater = {},
        )
    }
}

@Preview(device = Devices.NEXUS_5, showSystemUi = true, showBackground = true)
@Composable
fun SmallGetLiveWithUserUIPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        GetLiveWithUserUI(
            title = "Activate Neo",
            message = "Activating New as an approver will take about 2 minutes. This activation should preferable take place while you're on the phone or in-person to ensure you are activating the proper approver.",
            buttonText = "Verify Now",
            onContinueLive = {},
            onResumeLater = {},
        )
    }
}