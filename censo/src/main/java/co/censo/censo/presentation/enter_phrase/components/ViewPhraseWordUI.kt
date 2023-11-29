package co.censo.censo.presentation.enter_phrase.components

import StandardButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.presentation.components.YesNoDialog
import co.censo.shared.R
import co.censo.shared.presentation.SharedColors

@Composable
fun BoxScope.ViewPhraseWordUI(
    editedWordIndex: Int,
    phraseWord: String,
    editExistingWord: () -> Unit,
    decrementEditIndex: () -> Unit,
    incrementEditIndex: () -> Unit,
    enterNextWord: () -> Unit,
    submitFullPhrase: () -> Unit,
    deleteExistingWord: () -> Unit
) {
    val confirmDeleteWord = remember { mutableStateOf(false) }

    when (confirmDeleteWord.value) {
        true ->
            YesNoDialog(
                title = stringResource(id = co.censo.censo.R.string.delete_word),
                message = stringResource(co.censo.censo.R.string.are_you_sure_you_want_to_delete_this_word),
                onDismiss = { confirmDeleteWord.value = false }) {
                confirmDeleteWord.value = false
                deleteExistingWord()
            }
        false -> {
            Box(modifier = Modifier.align(Alignment.Center)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        modifier = Modifier
                            .weight(0.15f)
                            .padding(start = 8.dp),
                        onClick = decrementEditIndex
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_left),
                            contentDescription = stringResource(id = co.censo.censo.R.string.move_one_word_back),
                            tint = Color.Black
                        )
                    }
                    ViewPhraseWord(
                        modifier = Modifier.weight(0.7f),
                        index = editedWordIndex,
                        phraseWord = phraseWord,
                        editWord = editExistingWord,
                        deleteWord = {
                            confirmDeleteWord.value = true
                        }
                    )
                    IconButton(
                        modifier = Modifier
                            .weight(0.15f)
                            .padding(end = 8.dp),
                        onClick = incrementEditIndex
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_right),
                            contentDescription = stringResource(id = co.censo.censo.R.string.move_one_word_back),
                            tint = Color.Black
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(color = SharedColors.DividerGray)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row {
                    Spacer(modifier = Modifier.width(12.dp))
                    StandardButton(
                        modifier = Modifier.weight(0.65f),
                        color = Color.Black,
                        contentPadding = PaddingValues(
                            horizontal = 24.dp, vertical = 16.dp
                        ),
                        onClick = enterNextWord
                    ) {
                        Text(
                            fontSize = 20.sp,
                            text = stringResource(co.censo.censo.R.string.enter_next_word),
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    StandardButton(
                        modifier = Modifier.weight(0.35f),
                        color = Color.Black,
                        contentPadding = PaddingValues(
                            horizontal = 24.dp, vertical = 16.dp
                        ),
                        onClick = submitFullPhrase
                    ) {
                        Text(
                            fontSize = 20.sp,
                            text = stringResource(co.censo.censo.R.string.finish),
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewViewPhraseWordUI() {
    Box(modifier = Modifier.fillMaxSize()) {
        ViewPhraseWordUI(
            editedWordIndex = 5,
            phraseWord = "lounge",
            editExistingWord = { },
            decrementEditIndex = { },
            incrementEditIndex = { },
            enterNextWord = { },
            submitFullPhrase = { },
            deleteExistingWord = { })
    }
}
