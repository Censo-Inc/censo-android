package co.censo.censo.presentation.biometry_reset.components

import StandardButton
import TitleText
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors

@Composable
fun ReEnrollBiometryUI(
    onEnrollBiometry: () -> Unit,
) {

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(color = Color.White),
        verticalArrangement = Arrangement.Bottom
    ) {

        Box(modifier = Modifier
            .fillMaxWidth()
            .weight(0.1f)
        ) {
            Image(
                modifier = Modifier
                    .padding(top = screenHeight * 0.015f)
                    .align(Alignment.Center),
                painter = painterResource(id = R.drawable.face_scan_hand_with_phone),
                contentDescription = null,
                contentScale = ContentScale.Fit
            )
        }

        Column(modifier = Modifier.padding(horizontal = 44.dp)) {
            TitleText(
                title = R.string.update_scan_your_face,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(screenHeight * 0.025f))
            Text(
                text = stringResource(R.string.update_scan_your_face_blurb),
                fontWeight = FontWeight.Light,
                fontStyle = FontStyle.Italic,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                color = SharedColors.MainColorText,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(screenHeight * 0.025f))
            StandardButton(
                onClick = onEnrollBiometry,
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(id = co.censo.shared.R.drawable.small_face_scan_white),
                        contentDescription = null,
                        modifier = Modifier.width(32.dp),
                        colorFilter = ColorFilter.tint(SharedColors.ButtonTextBlue)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.begin_face_scan),
                        style = ButtonTextStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.Medium),
                    )
                }
            }
            Spacer(modifier = Modifier.height(screenHeight * 0.025f))
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ReEnrollBiometryUIPreview() {
    ReEnrollBiometryUI(onEnrollBiometry = {})
}
