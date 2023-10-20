package co.censo.vault.presentation.enter_phrase.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.presentation.SharedColors
import co.censo.vault.R
import co.censo.vault.presentation.enter_phrase.components.PhraseWordUtil.START_FILTER_INDEX
import co.censo.vault.util.BIP39

object PhraseWordUtil {
    const val START_FILTER_INDEX = 1
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhraseEntryTextField(
    phrase: String,
    wordSelected: Boolean,
    onPhraseUpdated: (String) -> Unit,
    onWordSelected: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val displayAutocomplete = remember { mutableStateOf(true) }

    LaunchedEffect(key1 = Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            value = phrase,
            onValueChange = {
                if (it != phrase) {
                    displayAutocomplete.value = true
                    onPhraseUpdated(it)
                }
            },
            singleLine = true,
            isError = false,
            trailingIcon = {
                Box {
                    IconButton(onClick = { onPhraseUpdated("") }) {
                        Icon(
                            painterResource(id = co.censo.shared.R.drawable.erase_text),
                            contentDescription = stringResource(R.string.clear_word_entry),
                            tint = Color.Black
                        )
                    }
                }
            },
            textStyle = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.W500
            ),
            enabled = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            shape = RoundedCornerShape(0.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color.Black
            )
        )
        PhraseAutoCompleteWords(
            phrase = phrase,
            showAutoComplete = phrase.length >= START_FILTER_INDEX,
            wordSelected = wordSelected
        ) { wordTapped ->
            onWordSelected(wordTapped)
            focusRequester.freeFocus()
            displayAutocomplete.value = false
        }
    }
}

@Composable
fun PhraseAutoCompleteWords(
    phrase: String,
    showAutoComplete: Boolean,
    wordSelected: Boolean,
    onWordTap: (String) -> Unit
) {

    val textStartPadding = 16.dp

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
        if (showAutoComplete) {
            val potentialWords =
                BIP39.wordlists[BIP39.WordList.English]!!.filter { it.startsWith(phrase.lowercase().trim()) }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                modifier = Modifier.padding(start = textStartPadding),
                textAlign = TextAlign.Start,
                text = stringResource(R.string.of_2_048_potential_words, potentialWords.size),
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                Text(
                    modifier = Modifier.padding(start = textStartPadding),
                    text = annotatedString
                )
            } else {
                for (word in potentialWords) {

                    val basicStyle = SpanStyle(
                        color = Color.Black,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.W300
                    )

                    val potentialWordText = buildAnnotatedString {
                        withStyle(basicStyle.copy(fontWeight = FontWeight.W500)) {
                            append(word.slice(phrase.indices))
                        }

                        if (word.length != phrase.length) {
                            withStyle(basicStyle) {
                                append(word.slice(phrase.length until word.length))
                            }
                        }
                    }

                    ClickableText(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = textStartPadding,
                                top = 8.dp,
                                bottom = 8.dp
                            ),
                        text = potentialWordText,
                        onClick = { onWordTap(word) }
                    )

                    if (wordSelected) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = textStartPadding),
                            text = stringResource(R.string.valid_seed_phrase_word),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.W500,
                            color = SharedColors.SuccessGreen
                        )
                    }
                }
            }
        }
    }

}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewPhraseEntry() {
    PhraseEntryTextField(
        phrase = "Ca",
        onPhraseUpdated = {},
        wordSelected = false,
    ) {

    }
}