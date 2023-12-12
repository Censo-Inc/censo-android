package co.censo.censo.presentation.enter_phrase.components

import StandardButton
import TitleText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.presentation.SharedColors
import co.censo.censo.R
import co.censo.shared.util.BIP39

private val wordCountOptions = BIP39.WordCount.values().toList()

@Composable
fun GeneratePhraseUI(
    selectedWordCount: BIP39.WordCount,
    onWordCountSelected: (BIP39.WordCount) -> Unit,
    onGenerate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        Column {
            Spacer(modifier = Modifier.height(32.dp))
            TitleText(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                title = R.string.select_phrase_length
            )
            Spacer(modifier = Modifier.height(56.dp))
        }

        wordCountOptions.forEach { wordCount ->
            WordCountOption(
                wordCount = wordCount,
                selected = (wordCount == selectedWordCount),
                onSelect = { onWordCountSelected(wordCount) }
            )
        }

        StandardButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 24.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            onClick = onGenerate,
        ) {
            Text(
                text = stringResource(R.string.generate),
                color = Color.White,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
private fun WordCountOption(
    wordCount: BIP39.WordCount,
    selected: Boolean,
    onSelect: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp)
            .background(
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent
            )
            .border(
                width = 1.dp,
                color = if (selected) Color.Black else SharedColors.BorderGrey,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clickable { onSelect() },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Box(
            modifier = Modifier
                .width(25.dp)
                .align(Alignment.CenterVertically),
        ) {
            if (selected) {
                Icon(
                    painterResource(id = co.censo.shared.R.drawable.check_icon),
                    contentDescription = stringResource(R.string.select_approver),
                    tint = Color.Black
                )
            }
        }

        Column {
            Text(
                text = stringResource(R.string.seed_phrase_words_count, wordCount.value),
                color = Color.Black,
                fontSize = 24.sp
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewGeneratePhraseUI() {
    GeneratePhraseUI(
        selectedWordCount = BIP39.WordCount.TwentyFour,
        onWordCountSelected = {},
        onGenerate = {}
    )
}