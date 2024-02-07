package co.censo.shared.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.R
import co.censo.shared.presentation.SharedColors

@Composable
fun PostSuccessAction() {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val aspectRatio = screenHeight / screenWidth

    val (padding, fontSize) = when {
        aspectRatio > 1.9f -> (screenWidth * 0.05f to 28.sp)
        else -> (screenWidth * 0.08f to 24.sp)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Spacer(modifier = Modifier.weight(0.3f))
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                modifier = Modifier.align(Alignment.Center),
                painter = painterResource(id = R.drawable.post_success_congrats),
                contentDescription = null,
                contentScale = ContentScale.Fit
            )
        }
        Spacer(modifier = Modifier.weight(0.1f))
        Text(
            text = stringResource(R.string.thanks_for_helping_someone),
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = SharedColors.MainColorText,
            lineHeight = 30.sp,
            modifier = Modifier.padding(horizontal = 25.dp)
        )
        Spacer(modifier = Modifier.weight(0.1f))
        Text(
            text = stringResource(R.string.you_may_now_close_the_app),
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = SharedColors.MainColorText,
            lineHeight =  30.sp,
            modifier = Modifier.padding(horizontal = 25.dp)
        )
        Spacer(modifier = Modifier.weight(0.4f))
    }
}

@Preview(device = Devices.PIXEL_4_XL, showSystemUi = true, showBackground = true)
@Composable
fun LargePostSuccessActionPreview() {
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)) {
        PostSuccessAction()
    }
}

@Preview(device = Devices.PIXEL_4, showSystemUi = true, showBackground = true)
@Composable
fun MediumPostSuccessActionPreview() {
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)) {
        PostSuccessAction()
    }
}

@Preview(device = Devices.NEXUS_5, showSystemUi = true, showBackground = true)
@Composable
fun SmallPostSuccessActionPreview() {
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)) {
        PostSuccessAction()
    }
}