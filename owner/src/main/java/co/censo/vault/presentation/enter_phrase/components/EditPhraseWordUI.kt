package co.censo.vault.presentation.enter_phrase.components

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
import co.censo.vault.R
import co.censo.vault.presentation.enter_phrase.EnterPhraseUIState

@Composable
fun BoxScope.EditPhraseWordUI(
    phraseWord: String,
    enterWordUIState: EnterPhraseUIState,
    updateEditedWord: (String) -> Unit,
    onWordSelected: (String) -> Unit,
    wordSubmitted: () -> Unit
) {
    PhraseEntryTextField(
        phrase = phraseWord,
        onPhraseUpdated = updateEditedWord,
        onWordSelected = onWordSelected,
        wordSelected = enterWordUIState == EnterPhraseUIState.SELECTED
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
                color = Color.Black, contentPadding = PaddingValues(
                    horizontal = 32.dp, vertical = 16.dp
                ), onClick = wordSubmitted
            ) {
                Text(
                    fontSize = 20.sp,
                    text = stringResource(R.string.submit_word),
                    color = Color.White
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
            onWordSelected = {}
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
            onWordSelected = {}
        ) {

        }
    }
}