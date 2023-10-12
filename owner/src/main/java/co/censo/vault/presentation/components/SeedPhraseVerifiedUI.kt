package co.censo.vault.presentation.components

import FullScreenButton
import LearnMore
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cash.z.ecc.android.bip39.Mnemonics
import co.censo.shared.presentation.SharedColors
import co.censo.shared.util.projectLog
import co.censo.vault.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SeedPhraseVerificationReview(
    valid: Boolean,
    phraseWords: List<String>,
    flipPhraseValidity: () -> Unit
) {

    val drawableResource = if (valid) R.drawable.check_circle else R.drawable.warning
    val title = if (valid) R.string.seed_phrase_verified else R.string.review_seed_phrase

    val message =
        if (valid) R.string.censo_has_verified_that_this_is_a_valid_seed_phrase
        else R.string.censo_has_detected_that_this_is_not_a_valid_seed_phrase

    val buttonText = if (valid) R.string.save_seed_phrase else R.string.submit_again

    val horizontalPadding = 32.dp

    val pagerState = rememberPagerState(0)

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Image(
            painterResource(id = drawableResource),
            contentDescription = ""
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            modifier = Modifier.padding(horizontal = horizontalPadding).clickable { flipPhraseValidity() },
            text = stringResource(id = title),
            color = Color.Black,
            fontSize = 24.sp,
            fontWeight = FontWeight.W600,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            modifier = Modifier.padding(horizontal = horizontalPadding),
            text = stringResource(id = message),
            color = Color.Black,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val coroutineScope = rememberCoroutineScope()

            IconButton(
                modifier = Modifier.weight(0.1f).padding(start = 8.dp),
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                }) {
                Icon(
                    painter = painterResource(co.censo.shared.R.drawable.arrow_left),
                    contentDescription = "scroll one word left",
                    tint = Color.Black
                )
            }
            HorizontalPager(
                modifier = Modifier
                    .weight(0.8f)
                    .padding(horizontal = 24.dp),
                pageCount = phraseWords.size,
                state = pagerState
            ) { page ->
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .background(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(
                            border = BorderStroke(1.dp, Color.Black),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${page + 1}${getSuffixForWordIndex(page + 1)} word",
                        fontSize = 20.sp
                    )
                    Text(
                        text = phraseWords[page],
                        modifier = Modifier
                            .padding(all = 48.dp)
                            .fillMaxWidth(),
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.W600
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            IconButton(
                modifier = Modifier.weight(0.1f).padding(end = 8.dp),
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }) {
                Icon(
                    painter = painterResource(co.censo.shared.R.drawable.arrow_right),
                    contentDescription = "scroll one word right",
                    tint = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        FullScreenButton(
            modifier = Modifier.padding(horizontal = horizontalPadding + 12.dp),
            color = Color.Black,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            onClick = { /*TODO*/ }) {
            Text(
                text = stringResource(id = buttonText),
                color = Color.White,
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        LearnMore {
            Toast.makeText(context, "Show FAQ Webview", Toast.LENGTH_LONG).show()
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

fun getSuffixForWordIndex(index: Int): String =
    when (index) {
        1, 21 -> "st"
        2, 22 -> "nd"
        3, 23 -> "rd"
        in (4..20) -> "th"
        else -> "th"
    }

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewSeedPhraseVerification() {

    val words = String(Mnemonics.MnemonicCode(Mnemonics.WordCount.COUNT_24).chars).split(" ")

    SeedPhraseVerificationReview(
        valid = true,
        phraseWords = words.toList()
    ) {

    }
}