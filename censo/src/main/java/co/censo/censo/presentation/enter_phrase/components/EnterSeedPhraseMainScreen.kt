package co.censo.censo.presentation.enter_phrase.components

import StandardButton
import androidx.compose.foundation.layout.Column
import MessageText
import TitleText
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
import co.censo.shared.util.BIP39
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors

@Composable
fun SelectSeedPhraseEntryType(
    welcomeFlow: Boolean,
    currentLanguage: BIP39.WordListLanguage,
    onManualEntrySelected: (selectedLanguage: BIP39.WordListLanguage) -> Unit,
    onPasteEntrySelected: () -> Unit,
    onGenerateEntrySelected: () -> Unit
) {

    var selectedLanguage by remember { mutableStateOf(currentLanguage) }


    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val verticalSpacingHeight = screenHeight * 0.020f


    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = screenWidth * 0.15f, top = screenHeight * 0.15f),
            painter = painterResource(id = R.drawable.addyourseedphrase),
            contentDescription = null,
            contentScale = ContentScale.Fit
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            val title =
                if (welcomeFlow) R.string.add_first_seed_phrase else R.string.add_seed_phrase

            TitleText(
                modifier = Modifier.fillMaxWidth(),
                title = title,
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(verticalSpacingHeight))

            MessageText(
                modifier = Modifier.fillMaxWidth(),
                message = R.string.add_seed_phrase_message,
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(12.dp))

            val basicStyle = SpanStyle(
                color = SharedColors.MainColorText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )

            val languageSelectionText = buildAnnotatedString {
                withStyle(basicStyle) {
                    append(
                        stringResource(
                            R.string.current_language,
                            selectedLanguage.displayName()
                        )
                    )
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

            Spacer(modifier = Modifier.height(verticalSpacingHeight))

            StandardButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onManualEntrySelected(selectedLanguage)
                },
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.manual_entry_icon),
                        contentDescription = null,
                        tint = SharedColors.ButtonTextBlue
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.input_seed_phrase),
                        style = ButtonTextStyle.copy(fontSize = 20.sp, fontWeight = null)
                    )
                }
            }

            Spacer(modifier = Modifier.height(verticalSpacingHeight))

            StandardButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onGenerateEntrySelected,
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 32.dp)
            ) {
                Row {
                    Icon(
                        painter = painterResource(id = co.censo.shared.R.drawable.wand_and_stars),
                        contentDescription = null,
                        tint = SharedColors.ButtonTextBlue
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.generate_seed_phrase),
                        style = ButtonTextStyle.copy(fontSize = 20.sp, fontWeight = null)
                    )
                }
            }

            Spacer(modifier = Modifier.height(verticalSpacingHeight))

            StandardButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onPasteEntrySelected,
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = co.censo.shared.R.drawable.paste_phrase_icon),
                        contentDescription = null,
                        tint = SharedColors.ButtonTextBlue
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.paste_seed_phrase),
                        style = ButtonTextStyle.copy(fontSize = 20.sp, fontWeight = null)
                    )
                }
            }

            Spacer(modifier = Modifier.height(verticalSpacingHeight))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewEnterPhraseMainScreen() {
    SelectSeedPhraseEntryType(
        onManualEntrySelected = {},
        onPasteEntrySelected = {},
        onGenerateEntrySelected = {},
        welcomeFlow = false,
        currentLanguage = BIP39.WordListLanguage.English
    )
}