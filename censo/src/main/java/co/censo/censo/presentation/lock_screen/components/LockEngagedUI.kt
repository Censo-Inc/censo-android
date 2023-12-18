package co.censo.censo.presentation.lock_screen.components

import StandardButton
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors

@Composable
fun LockEngagedUI(
    initUnlock: () -> Unit
) {

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    val bigSpacer = screenHeight * 0.05f
    val mediumSpacer = screenHeight * 0.035f
    val smallSpacer = screenHeight * 0.010f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = SharedColors.LockScreenBackground),
        contentAlignment = Alignment.BottomCenter
    ) {

        Image(
            modifier = Modifier.fillMaxWidth(),
            painter = painterResource(id = R.drawable.shortened_dog),
            contentDescription = null,
            contentScale = ContentScale.Fit,
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(color = Color.Transparent)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Spacer(modifier = Modifier.height(bigSpacer))

        Image(
            modifier = Modifier.size(100.dp),
            painter = painterResource(id = R.drawable.censo_login_logo),
            contentDescription = stringResource(R.string.app_content_is_locked_behind_facescan),
        )

        Spacer(modifier = Modifier.height(smallSpacer))

        val textStyle = TextStyle(
            color = SharedColors.MainColorText,
            fontSize = 32.sp,
            fontWeight = FontWeight.W600,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Welcome back.",
            style = textStyle
        )

        Spacer(modifier = Modifier.height(smallSpacer))

        Line(
            length = 36.dp,
            height = 6.dp
        )

        Spacer(modifier = Modifier.height(smallSpacer))

        Text(
            text = "The Seed Phrase Manager that lets you sleep at night.",
            style = textStyle
        )

        Spacer(modifier = Modifier.height(mediumSpacer))

        StandardButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            onClick = initUnlock,
        ) {
            Text(
                text = stringResource(id = R.string.continue_text),
                style = ButtonTextStyle.copy(fontSize = 20.sp)
            )
        }
    }
}

@Composable
fun Line(length: Dp, height: Dp) {
    Spacer(
        modifier = Modifier
            .width(length)
            .height(height)
            .background(color = SharedColors.LightColorLine)
    )
}

@Preview(device = Devices.PIXEL_4_XL, showSystemUi = true, showBackground = true)
@Composable
fun LargePreviewLockScreen() {
    LockEngagedUI {

    }
}

@Preview(device = Devices.PIXEL_4, showSystemUi = true, showBackground = true)
@Composable
fun NormalPreviewLockScreen() {
    LockEngagedUI {

    }
}

@Preview(device = Devices.NEXUS_5, showSystemUi = true, showBackground = true)
@Composable
fun SmallPreviewLockScreen() {
    LockEngagedUI {

    }
}
