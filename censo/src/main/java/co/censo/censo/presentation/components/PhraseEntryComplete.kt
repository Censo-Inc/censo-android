package co.censo.censo.presentation.components

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
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors

@Composable
fun SeedPhraseAdded(
    isSavingFirstSeedPhrase: Boolean,
    onClick: () -> Unit,
) {

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Spacer(modifier = Modifier.height(screenHeight * 0.05f))

        Image(
            modifier = Modifier.fillMaxWidth().weight(0.1f),
            painter = painterResource(id = co.censo.censo.R.drawable.congrats),
            contentDescription = null,
            contentScale = ContentScale.Fit,
        )

        Spacer(modifier = Modifier.height(screenHeight * 0.05f))

        val messageText =
            if (isSavingFirstSeedPhrase) stringResource(co.censo.censo.R.string.phrase_added_done_message)
            else stringResource(co.censo.censo.R.string.subsequent_seed_phrase_saved)

        Text(
            modifier = Modifier.padding(horizontal = screenWidth * 0.05f),
            text = messageText,
            fontSize = 26.sp,
            color = SharedColors.MainColorText,
            fontWeight = FontWeight.Bold,
            lineHeight = 30.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(screenHeight * 0.05f))

        StandardButton(
            modifier = Modifier.fillMaxWidth().padding(horizontal = screenWidth * 0.05f),
            onClick = onClick,
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 32.dp)
        ) {
            Text(
                text = stringResource(co.censo.censo.R.string.ok),
                style = ButtonTextStyle.copy(fontSize = 24.sp, fontWeight = FontWeight.W400)
            )
        }

        Spacer(modifier = Modifier.height(screenHeight * 0.05f))
    }
}

@Preview(device = Devices.PIXEL_4_XL, showSystemUi = true, showBackground = true)
@Composable
fun LargeSeedPhraseAddedPreview() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        SeedPhraseAdded(true) {}
    }
}

@Preview(device = Devices.PIXEL_4, showSystemUi = true, showBackground = true)
@Composable
fun NormalSeedPhraseAddedPreview() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        SeedPhraseAdded(true) {}
    }
}

@Preview(device = Devices.NEXUS_5, showSystemUi = true, showBackground = true)
@Composable
fun SmallSeedPhraseAddedPreview() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        SeedPhraseAdded(true) {}
    }
}