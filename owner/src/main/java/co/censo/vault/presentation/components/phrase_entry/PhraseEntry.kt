package co.censo.vault.presentation.components.phrase_entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cash.z.ecc.android.bip39.Mnemonics
import co.censo.vault.R
import co.censo.vault.presentation.components.phrase_entry.Bip39Words.words
import co.censo.vault.presentation.components.phrase_entry.PhraseWordUtil.START_FILTER_INDEX
import java.util.Locale

object Bip39Words {
    val words = Mnemonics.getCachedWords(Locale.ENGLISH.language)
}

object PhraseWordUtil {
    const val START_FILTER_INDEX = 2
}

enum class PhraseEntryIcon {
    CLEAR, ADDED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhraseEntryTextField(
    phrase: TextFieldValue,
    onPhraseUpdated: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val displayAutocomplete = remember { mutableStateOf(true) }

    LaunchedEffect(key1 = Unit) {
        focusRequester.requestFocus()
    }

    val endIconType = if (displayAutocomplete.value) {
        PhraseEntryIcon.CLEAR
    } else if (phrase.text in words) {
        PhraseEntryIcon.ADDED
    } else {
        PhraseEntryIcon.CLEAR
    }

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(36.dp))
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .focusRequester(focusRequester),
            value = phrase,
            onValueChange = {
                if (it.text != phrase.text) {
                    displayAutocomplete.value = true
                    onPhraseUpdated(it.text)
                }
            },
            singleLine = true,
            isError = false,
            placeholder = {
                Text(text = stringResource(R.string.enter_word), color = Color.Black)
            },
            trailingIcon = {
                Box {
                    val iconClear = (endIconType == PhraseEntryIcon.CLEAR)
                    IconButton(onClick = { onPhraseUpdated("") }) {
                        Icon(
                            imageVector = if (iconClear) Icons.Default.Cancel else Icons.Default.CheckCircle,
                            contentDescription = if (iconClear) stringResource(R.string.clear_word_entry) else stringResource(
                                R.string.valid_word
                            ),
                            tint = Color.Black
                        )
                    }
                }
            },
            enabled = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        Spacer(modifier = Modifier.height(36.dp))
        Divider(modifier = Modifier.height(1.dp))
        PhraseAutoCompleteWords(
            phrase = phrase.text,
            showAutoComplete = displayAutocomplete.value && phrase.text.length >= START_FILTER_INDEX
        ) { wordTapped ->
            onPhraseUpdated(wordTapped)
            focusRequester.freeFocus()
            displayAutocomplete.value = false
        }
    }
}

@Composable
fun PhraseAutoCompleteWords(
    phrase: String,
    showAutoComplete: Boolean,
    onWordTap: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {

        if (showAutoComplete) {
            val potentialWords =
                Bip39Words.words.filter { it.startsWith(phrase.lowercase().trim()) }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                textAlign = TextAlign.Start,
                text = stringResource(R.string.of_2_048_potential_words, potentialWords.size),
                fontSize = 18.sp
            )

            Divider(modifier = Modifier.height(1.dp))

            Spacer(modifier = Modifier.height(12.dp))

            if (potentialWords.isEmpty()) {

                val basicStyle = SpanStyle(
                    color = Color.Black,
                    fontSize = 24.sp
                )

                val annotatedString = buildAnnotatedString {
                    withStyle(basicStyle) {
                        append(stringResource(R.string.sorry))
                    }
                    withStyle(basicStyle.copy(fontWeight = FontWeight.W600)) {
                        append(phrase)
                    }
                    withStyle(basicStyle) {
                        append(stringResource(R.string.is_not_a_valid_seed_phrase_word))
                    }
                }
                Text(text = annotatedString)
            } else {
                for (word in potentialWords) {

                    val basicStyle = SpanStyle(
                        color = Color.Black,
                        fontSize = 24.sp
                    )

                    val potentialWordText = buildAnnotatedString {
                        withStyle(basicStyle.copy(fontWeight = FontWeight.W600)) {
                            append(word.slice(phrase.indices))
                        }

                        if (word.length != phrase.length) {
                            withStyle(basicStyle) {
                                append(word.slice(phrase.length until word.length))
                            }
                        }
                    }

                    ClickableText(
                        text = potentialWordText,
                        onClick = { onWordTap(word) }
                    )
                }
            }
        }
    }

}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewPhraseEntry() {
    PhraseEntryTextField(
        phrase = TextFieldValue("Ca", selection = TextRange(0, 1)),
    ) {

    }
}