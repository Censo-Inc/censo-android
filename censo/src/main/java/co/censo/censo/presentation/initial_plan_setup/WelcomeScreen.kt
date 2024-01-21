package co.censo.censo.presentation.initial_plan_setup

import StandardButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.presentation.SharedColors
import co.censo.censo.R
import co.censo.shared.presentation.ButtonTextStyle

@Composable
fun WelcomeScreenUI(
    navigateToPlanSetup: () -> Unit
) {
    Column(
        Modifier
            .background(color = Color.White)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.welcome_to_censo),
                fontSize = 44.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                color = Color.Black,
                lineHeight = 48.sp,
                modifier = Modifier
                    .padding(start = 32.dp, top = 24.dp, bottom = 24.dp, end = 12.dp)
                    .fillMaxWidth()
            )
            Text(
                stringResource(id = R.string.welcome_blurb),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Start,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            SetupStep(
                imagePainter = painterResource(id = co.censo.shared.R.drawable.google),
                heading = stringResource(R.string.authenticate_privately),
                content = stringResource(R.string.authenticate_privately_blurb),
                completionText = stringResource(R.string.authenticated)
            )
            SetupStep(
                imagePainter = painterResource(id = co.censo.shared.R.drawable.small_face_scan),
                heading = stringResource(id = R.string.scan_your_face),
                content = stringResource(id = R.string.scan_your_face_blurb),
            )
            SetupStep(
                imagePainter = painterResource(id = co.censo.shared.R.drawable.phrase_entry),
                heading = stringResource(id = R.string.enter_your_seed_phrase),
                content = stringResource(id = R.string.enter_your_phrase_blurb),
            )
        }

        StandardButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 44.dp, vertical = 24.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            onClick = navigateToPlanSetup,
        ) {
            Text(
                text = stringResource(id = R.string.get_started),
                style = ButtonTextStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(all = 8.dp)
            )
        }
    }
}

@Composable
fun SetupStep(
    imagePainter: Painter,
    heading: String,
    content: String,
    imageBackgroundColor: Color = SharedColors.BackgroundGrey,
    iconColor: Color = Color.Black,
    completionText: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            contentAlignment = Alignment.Center, modifier = Modifier
                .background(color = imageBackgroundColor, shape = RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Icon(
                painter = imagePainter,
                contentDescription = null,
                modifier = Modifier.width(32.dp),
                tint = iconColor
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = heading,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                fontSize = 14.sp,
                lineHeight = 16.0.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (completionText != null) {
                Text(
                    text = "✓ $completionText",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SharedColors.SuccessGreen,
                )
            }
        }
    }
}

@Preview(device = Devices.NEXUS_5, showSystemUi = true, showBackground = true)
@Composable
fun SmallWelcomeScreenUIPreview() {
    WelcomeScreenUI {

    }
}

@Preview(device = Devices.PIXEL_4, showSystemUi = true, showBackground = true)
@Composable
fun MediumWelcomeScreenUIPreview() {
    WelcomeScreenUI {

    }
}

@Preview(device = Devices.PIXEL_4_XL, showSystemUi = true, showBackground = true)
@Composable
fun LargeWelcomeScreenUIPreview() {
    WelcomeScreenUI {

    }
}