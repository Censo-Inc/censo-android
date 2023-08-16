package co.censo.vault.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import co.censo.vault.R
import co.censo.vault.TestTag

@Composable
fun PhraseWords(
    phraseWords: List<IndexedPhraseWord> = emptyList(),
    biometryError: Boolean,
    retry: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 48.dp)
            .border(width = 1.dp, color = Color.Gray)
            .background(color = Color.Black.copy(alpha = 0.25f))
            .zIndex(2.5f),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (biometryError) {
            Text(text = stringResource(R.string.please_complete_biometry_to_continue))
            Spacer(modifier = Modifier.height(24.dp))
            VaultButton(onClick = retry) {
                Text(text = stringResource(id = R.string.try_again))
            }
        } else if (phraseWords.isEmpty()) {
            Text(
                modifier = Modifier.semantics { testTag = TestTag.bip_39_detail_biometry_text },
                text = stringResource(R.string.phrase_words_empty),
                color = Color.White,
                textAlign = TextAlign.Center,
                fontSize = 20.sp
            )
        } else {
            for ((localIndex, indexWord) in phraseWords.withIndex()) {
                //First Spacer
                if (localIndex == PhraseUICompanion.FIRST_SPACER_INDEX) {
                    Spacer(
                        modifier = Modifier.height(
                            dimensionResource(R.dimen.medium_word_spacer)
                        )
                    )
                }

                //Word Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 44.dp)
                        .semantics { testTag = TestTag.bip_39_detail_phrase_ui },
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getDisplayIndex(indexWord.wordIndex),
                        color = Color.Gray,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(34.dp))
                    Text(
                        text = indexWord.wordValue,
                        color = Color.White,
                        fontSize = 28.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                //Row Spacers
                if (localIndex != PhraseUICompanion.LAST_SPACER_INDEX) {
                    Spacer(
                        modifier = Modifier.height(
                            dimensionResource(R.dimen.large_word_spacer)
                        )
                    )
                }

                //Last Spacer
                if (localIndex == PhraseUICompanion.LAST_SPACER_INDEX) {
                    Spacer(
                        modifier = Modifier.height(
                            dimensionResource(R.dimen.medium_word_spacer)
                        )
                    )
                }
            }
        }
    }
}

fun getDisplayIndex(wordIndex: Int) =
    if (wordIndex < PhraseUICompanion.DOUBLE_DIGIT_INDEX) {
        " $wordIndex"
    } else {
        wordIndex.toString()
    }

object PhraseUICompanion {
    const val FIRST_SPACER_INDEX = 0
    const val LAST_SPACER_INDEX = 3

    const val DOUBLE_DIGIT_INDEX = 10

    const val DISPLAY_RANGE_SET = 3
    const val OFFSET_INDEX_ZERO = 1
}

data class IndexedPhraseWord(
    val wordIndex: Int,
    val wordValue: String
)