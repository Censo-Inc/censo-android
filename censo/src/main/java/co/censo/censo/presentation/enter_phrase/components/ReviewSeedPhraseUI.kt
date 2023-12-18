package co.censo.censo.presentation.enter_phrase.components

import StandardButton
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors
import co.censo.shared.util.BIP39InvalidReason
import co.censo.shared.util.errorMessage
import co.censo.shared.util.errorTitle
import co.censo.censo.R as VaultR
import co.censo.shared.R as SharedR
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReviewSeedPhraseUI(
    phraseWords: List<String>,
    isGeneratedPhrase: Boolean = false,
    saveSeedPhrase: () -> Unit,
    editSeedPhrase: () -> Unit
) {

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp


    val title =
        if (isGeneratedPhrase) {
            stringResource(id = VaultR.string.seed_phrase_generated)
        } else {
            stringResource(id = VaultR.string.seed_phrase_validated)
        }


    val message =
        if (!isGeneratedPhrase) {
            stringResource(id = VaultR.string.censo_has_verified_that_this_is_a_valid_seed_phrase)
        } else ""

    val buttonText = VaultR.string.next

    val buttonAction = saveSeedPhrase

    val horizontalPadding = 32.dp

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { phraseWords.size }
    )

    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(),
            painter = painterResource(id = R.drawable.seedphrasevalidated),
            contentDescription = null,
            contentScale = ContentScale.Fit
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(screenHeight * 0.025f))

            Text(
                modifier = Modifier.padding(horizontal = horizontalPadding),
                text = title,
                fontSize = 28.sp,
                color = SharedColors.MainColorText,
                fontWeight = FontWeight.W500
            )

            Spacer(modifier = Modifier.height(screenHeight * 0.015f))

            Text(
                modifier = Modifier.padding(horizontal = horizontalPadding),
                text = message,
                fontSize = 16.sp,
                color = SharedColors.MainColorText,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.weight(0.75f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val coroutineScope = rememberCoroutineScope()

                IconButton(
                    modifier = Modifier
                        .weight(0.1f)
                        .padding(start = 8.dp),
                    onClick = {
                        if (pagerState.pageCount != 0) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    }) {
                    Icon(
                        painter = painterResource(co.censo.shared.R.drawable.arrow_left),
                        contentDescription = stringResource(VaultR.string.move_one_word_back),
                        tint = SharedColors.WordBoxIconTint
                    )
                }
                HorizontalPager(
                    modifier = Modifier
                        .weight(0.8f)
                        .padding(horizontal = 24.dp),
                    state = pagerState,
                    contentPadding = PaddingValues(0.dp),
                    beyondBoundsPageCount = 0,
                    key = null,
                    pageContent = {
                        ViewPhraseWord(
                            index = it,
                            phraseWord = phraseWords[it]
                        )
                    }
                )
                IconButton(
                    modifier = Modifier
                        .weight(0.1f)
                        .padding(end = 8.dp),
                    onClick = {
                        if (pagerState.pageCount != 0) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }) {
                    Icon(
                        painter = painterResource(co.censo.shared.R.drawable.arrow_right),
                        contentDescription = stringResource(VaultR.string.move_one_word_forward),
                        tint = SharedColors.WordBoxIconTint
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.15f))

            Text(
                text = stringResource(R.string.swipe_back_and_forth_to_review_words),
                color = SharedColors.MainColorText,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(screenHeight * 0.012f))

            StandardButton(
                modifier = Modifier
                    .padding(horizontal = horizontalPadding)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                onClick = buttonAction
            ) {
                Text(
                    text = stringResource(id = buttonText),
                    style = ButtonTextStyle
                )
            }

            Spacer(modifier = Modifier.height(screenHeight * 0.050f))
        }
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

    val words =
        "grocery crush fantasy pulse struggle brain federal equip remember figure lyrics afraid tape ugly gold yard way isolate drill lawn daughter either supply student".split(
            " "
        )

    ReviewSeedPhraseUI(
        phraseWords = words.toList(),
        saveSeedPhrase = {},
        editSeedPhrase = {}
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewGeneratedSeedPhraseVerification() {
    val words = "grocery crush fantasy pulse struggle brain federal equip remember figure lyrics afraid tape ugly gold yard way isolate drill lawn daughter either supply student".split(" ")

    ReviewSeedPhraseUI(
        phraseWords = words.toList(),
        isGeneratedPhrase = true,
        saveSeedPhrase = {},
        editSeedPhrase = {}
    )
}

@Composable
fun ViewPhraseWord(
    modifier: Modifier = Modifier,
    index: Int,
    phraseWord: String,
    editWord: (() -> Unit)? = null,
    deleteWord: (() -> Unit)? = null,
) {

    val context = LocalContext.current

    Column(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .background(
                color = SharedColors.WordBoxBackground,
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                border = BorderStroke(1.dp, SharedColors.WordBoxBorder),
                shape = RoundedCornerShape(24.dp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        val verticalSpacing = 20.dp

        Spacer(modifier = Modifier.height(verticalSpacing))
        Text(
            text = index.indexToWordText(context),
            fontSize = 20.sp,
            color = SharedColors.WordBoxTextColor
        )
        Spacer(modifier = Modifier.height(verticalSpacing))
        Text(
            text = phraseWord,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth(),
            fontSize = 28.sp,
            color = SharedColors.WordBoxTextColor,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.W600
        )
        Spacer(modifier = Modifier.height(verticalSpacing))
        if (editWord != null || deleteWord != null) {
            Row {
                editWord?.let {
                    IconButton(onClick = it) {
                        Icon(
                            painter = painterResource(id = co.censo.shared.R.drawable.edit_icon),
                            contentDescription = stringResource(VaultR.string.edit_word),
                            tint = SharedColors.WordBoxIconTint
                        )
                    }
                }
                deleteWord?.let {
                    IconButton(onClick = it) {
                        Icon(
                            painter = painterResource(id = co.censo.shared.R.drawable.trash),
                            contentDescription = stringResource(VaultR.string.delete_word),
                            tint = SharedColors.WordBoxIconTint
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(verticalSpacing))
        } else {
            Spacer(modifier = Modifier.height(verticalSpacing / 4))
        }
    }
}

fun Int.indexToWordText(context: Context) =
    context.getString(VaultR.string.word, this + 1, getSuffixForWordIndex(this + 1))

@Preview(showBackground = true)
@Composable
fun PreviewViewPhraseWord() {
    ViewPhraseWord(
        index = 3,
        phraseWord = "carbon"
    ) {

    }
}