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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.R

@Composable
fun SeedPhraseAdded(
    isSavingFirstSeedPhrase: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Image(
            painterResource(id = R.drawable.check_circle),
            contentDescription = null
        )

        Spacer(modifier = Modifier.weight(0.2f))

        Text(
            text = stringResource(co.censo.censo.R.string.congratulations),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(0.15f))


        val messageText =
            if (isSavingFirstSeedPhrase) stringResource(co.censo.censo.R.string.phrase_added_done_message)
            else stringResource(co.censo.censo.R.string.subsequent_seed_phrase_saved)

        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = messageText,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 36.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(0.55f))

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Black,
            onClick = onClick,
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 32.dp)
        ) {
            Text(
                text = stringResource(co.censo.censo.R.string.ok),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.W400
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Preview
@Composable
fun SeedPhraseAddedPreview() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        SeedPhraseAdded(true) {}
    }
}