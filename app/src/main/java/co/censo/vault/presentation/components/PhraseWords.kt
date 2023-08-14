package co.censo.vault.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import co.censo.vault.R
import co.censo.vault.presentation.components.PhraseUICompanion.DISPLAY_RANGE_SET

@Composable
fun WriteWordUI(
    phrase: String,
    index: Int,
    phraseSize: Int,
    changeWordIndex: (increasing: Boolean) -> Unit,
    retry: () -> Unit,
    failedBiometry: Boolean
) {

    val splitWords = phrase.split(" ")
    val wordsToShow = mutableListOf<IndexedPhraseWord>()

    if (splitWords.size >= index + DISPLAY_RANGE_SET) {
        for ((wordIndex, word) in splitWords.withIndex()) {
            if (wordIndex in index..index + DISPLAY_RANGE_SET) {
                wordsToShow.add(
                    IndexedPhraseWord(
                        wordIndex = wordIndex + PhraseUICompanion.OFFSET_INDEX_ZERO,
                        wordValue = word
                    )
                )
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .weight(1.0f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //region Title + Sub-title
            Spacer(
                modifier = Modifier.height(24.dp)
            )
            if (phrase.isNotEmpty()) {
                Text(
                    text = stringResource(
                        id = R.string.showing_of_words,
                        (index + 1).toString(),
                        (index + 1 + DISPLAY_RANGE_SET).toString(),
                        phraseSize
                    ),
                    color = Color.Gray,
                    fontSize = 18.sp
                )
            }
            Spacer(
                modifier = Modifier.height(48.dp)
            )
            //endregion

            //Phrase words
            PhraseWords(
                phraseWords = wordsToShow,
                biometryError = failedBiometry,
                retry = retry
            )

            //region Buttons
            Spacer(
                modifier = Modifier.height(15.dp)
            )

        }

        if (phrase.isNotEmpty()) {

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                //Buttons for displaying previous/next words
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                changeWordIndex(false)
                            }
                            .padding(end = 16.dp)
                    ) {
                        Icon(
                            modifier = Modifier.size(32.dp),
                            imageVector = Icons.Filled.NavigateBefore,
                            contentDescription = stringResource(R.string.previous_icon_content_desc),
                            tint = Color.Gray
                        )
                        Text(
                            text = stringResource(R.string.previous),
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                changeWordIndex(true)
                            }
                            .padding(start = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.next),
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Icon(
                            modifier = Modifier.size(32.dp),
                            imageVector = Icons.Filled.NavigateNext,
                            contentDescription = stringResource(R.string.next_icon_content_desc),
                            tint = Color.Gray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                //endregion
            }
        }
    }
}

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
                        .padding(horizontal = 44.dp),
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