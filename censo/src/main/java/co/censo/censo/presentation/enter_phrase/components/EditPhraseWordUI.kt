package co.censo.censo.presentation.enter_phrase.components

import StandardButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.presentation.SharedColors
import co.censo.censo.R
import co.censo.censo.presentation.enter_phrase.EnterPhraseUIState
import co.censo.shared.util.BIP39
import co.censo.shared.presentation.ButtonTextStyle

@Composable
fun BoxScope.EditPhraseWordUI(
    phraseWord: String,
    language: BIP39.WordListLanguage,
    enterWordUIState: EnterPhraseUIState,
    updateEditedWord: (String) -> Unit,
    onWordSelected: (String) -> Unit,
    wordSubmitted: () -> Unit
) {
    PhraseEntryTextField(
        phrase = phraseWord,
        onPhraseUpdated = updateEditedWord,
        onWordSelected = onWordSelected,
        wordSelected = enterWordUIState == EnterPhraseUIState.SELECTED,
        language = language
    )

    if (enterWordUIState == EnterPhraseUIState.SELECTED) {

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(color = Color.White),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(color = SharedColors.DividerGray)
            )
            Spacer(modifier = Modifier.height(24.dp))
            StandardButton(
                contentPadding = PaddingValues(
                    horizontal = 32.dp, vertical = 16.dp
                ), onClick = wordSubmitted
            ) {
                Text(
                    text = stringResource(R.string.submit_word),
                    style = ButtonTextStyle
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ViewPhraseWordPreview() {
    Box(modifier = Modifier.fillMaxSize()) {
        EditPhraseWordUI(
            phraseWord = "ban",
            enterWordUIState = EnterPhraseUIState.EDIT,
            updateEditedWord = {},
            onWordSelected = {},
            language = BIP39.WordListLanguage.English
        ) {

        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SelectedPhraseWordPreview() {
    Box(modifier = Modifier.fillMaxSize()) {

        EditPhraseWordUI(
            phraseWord = "banana",
            enterWordUIState = EnterPhraseUIState.SELECTED,
            updateEditedWord = {},
            onWordSelected = {},
            language = BIP39.WordListLanguage.English
        ) {

        }
    }
}