package co.censo.censo.presentation.access_seed_phrases.components

import StandardButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.censo.presentation.components.LanguageSelectionMenu
import co.censo.censo.presentation.welcome.SetupStep
import co.censo.shared.util.BIP39
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors

@Composable
fun ReadyToAccessPhrase(
    getStarted: (BIP39.WordListLanguage?) -> Unit
) {
    var selectedLanguage: BIP39.WordListLanguage? by remember { mutableStateOf(null) }

    Column(
        Modifier
            .background(color = Color.White)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 36.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            stringResource(id = R.string.ready_to_start),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start,
            color = SharedColors.MainColorText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        val basicStyle = SpanStyle(
            color = SharedColors.MainColorText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )

        val languageSelectionText = buildAnnotatedString {
            withStyle(basicStyle) {
                append(stringResource(R.string.seed_phrase_saved_language))
            }
            withStyle(basicStyle.copy(fontWeight = FontWeight.W600)) {
                append(stringResource(R.string.here))
            }
        }

        LanguageSelectionMenu(
            text = languageSelectionText,
            currentLanguage = selectedLanguage,
            action = {
                selectedLanguage = it
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            SetupStep(
                imagePainter = painterResource(id = R.drawable.keyhole_icon),
                heading = stringResource(R.string.private_place_title),
                content = stringResource(R.string.private_place_message),
            )
            SetupStep(
                imagePainter = painterResource(id = co.censo.shared.R.drawable.small_face_scan),
                heading = stringResource(R.string.scan_your_face),
                content = stringResource(R.string.access_time_scan_face_message),
            )
            SetupStep(
                imagePainter = painterResource(id = R.drawable.timer_icon),
                heading = stringResource(id = R.string.access_for_fifteen_title),
                content = stringResource(id = R.string.access_for_fifteen_message),
            )
        }

        StandardButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            onClick = {
                getStarted(selectedLanguage)
            },
        ) {
            Text(
                text = stringResource(id = R.string.get_started),
                style = ButtonTextStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(all = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewReadyToAccessPhrase() {
    ReadyToAccessPhrase {}
}