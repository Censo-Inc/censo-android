package co.censo.shared.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Device
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.R
import co.censo.shared.presentation.SharedColors

@Composable
fun ActionCompleteUI(title: String) {

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
    ) {

        Image(
            modifier = Modifier.align(Alignment.TopCenter),
            painter = painterResource(id = R.drawable.approved_confetti),
            contentDescription = null,
            contentScale = ContentScale.None,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Transparent),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(0.1f)) {
                Image(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = screenHeight * 0.10f),
                    painter = painterResource(id = R.drawable.top_approved_hand),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                )
            }

            Text(
                text = title,
                fontWeight = FontWeight.W400,
                color = SharedColors.MainColorText,
                fontSize = 48.sp
            )

            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(0.1f)) {
                Image(
                    modifier = Modifier
                        .align(Alignment.BottomStart),
                    painter = painterResource(id = R.drawable.bottom_approved_hand),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

@Preview(device = Devices.PIXEL_4, showSystemUi = true, showBackground = true)
@Composable
fun MediumActionCompleteUIPreview() {
    ActionCompleteUI("Approved!")
}

@Preview(device = Devices.PIXEL_4_XL, showSystemUi = true, showBackground = true)
@Composable
fun LargeActionCompleteUIPreview() {
    ActionCompleteUI("You are all set!")
}

@Preview(device = Devices.NEXUS_5, showSystemUi = true, showBackground = true)
@Composable
fun SmallActionCompleteUIPreview() {
    ActionCompleteUI("Approved!")
}