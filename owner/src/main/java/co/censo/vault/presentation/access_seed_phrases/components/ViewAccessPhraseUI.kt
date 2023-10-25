package co.censo.vault.presentation.access_seed_phrases.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.R
import co.censo.shared.presentation.SharedColors
import co.censo.vault.presentation.enter_phrase.components.ViewPhraseWord

@Composable
fun ViewAccessPhraseUI(
    wordIndex: Int,
    phraseWord: String,
    decrementIndex: () -> Unit,
    incrementIndex: () -> Unit,
) {

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "15 minutes left...", color = Color.Black)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                modifier = Modifier
                    .weight(0.15f)
                    .padding(start = 8.dp),
                onClick = decrementIndex
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_left),
                    contentDescription = stringResource(id = co.censo.vault.R.string.move_one_word_back),
                    tint = Color.Black
                )
            }
            ViewPhraseWord(
                modifier = Modifier.weight(0.7f),
                index = wordIndex,
                phraseWord = phraseWord,
            )
            IconButton(
                modifier = Modifier
                    .weight(0.15f)
                    .padding(end = 8.dp),
                onClick = incrementIndex
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_right),
                    contentDescription = stringResource(id = co.censo.vault.R.string.move_one_word_back),
                    tint = Color.Black
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(
                            color = SharedColors.DarkGreyBackground,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .weight(0.25f)
                        .padding(8.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(44.dp),
                        painter = painterResource(id = co.censo.vault.R.drawable.warning),
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(0.75f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Don't leave the app",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.W600,
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "You will need to start this process over if you leave or close the app.",
                        color = Color.Black,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewViewPhraseWordUI() {
    Box(modifier = Modifier.fillMaxSize()) {
        ViewAccessPhraseUI(
            wordIndex = 5,
            phraseWord = "lounge",
            decrementIndex = { },
            incrementIndex = { },
        )
    }
}