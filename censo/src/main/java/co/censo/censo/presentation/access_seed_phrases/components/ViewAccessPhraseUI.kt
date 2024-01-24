package co.censo.censo.presentation.access_seed_phrases.components

import StandardButton
import android.animation.TimeAnimator.TimeListener
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.R as SharedR
import co.censo.shared.presentation.SharedColors
import co.censo.censo.presentation.enter_phrase.components.ViewPhraseWord
import co.censo.shared.presentation.ButtonTextStyle
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ViewAccessPhraseUI(
    phraseWords: List<String>,
    onDone: () -> Unit,
    timeLeft: Duration
) {
    val context = LocalContext.current

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { phraseWords.size }
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

//        TimeleftForAccess()

        Spacer(modifier = Modifier.weight(1f))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val coroutineScope = rememberCoroutineScope()
            IconButton(
                modifier = Modifier
                    .weight(0.15f)
                    .padding(start = 8.dp),
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                }
            ) {
                Icon(
                    painter = painterResource(SharedR.drawable.arrow_left),
                    contentDescription = stringResource(id = co.censo.censo.R.string.move_one_word_back),
                    tint = SharedColors.MainIconColor
                )
            }
            HorizontalPager(
                modifier = Modifier.weight(0.7f),
                state = pagerState
            ) {
                ViewPhraseWord(
                    index = it,
                    phraseWord = phraseWords[it],
                )
            }
            IconButton(
                modifier = Modifier
                    .weight(0.15f)
                    .padding(end = 8.dp),
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            ) {
                Icon(
                    painter = painterResource(SharedR.drawable.arrow_right),
                    contentDescription = stringResource(id = co.censo.censo.R.string.move_one_word_back),
                    tint = SharedColors.MainIconColor
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.swipe_back_and_forth_to_review_words),
            fontSize = 14.sp,
            color = SharedColors.MainColorText
        )

        Spacer(modifier = Modifier.weight(1f))

        StandardButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp, end = 56.dp, top = 12.dp, bottom = 24.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            onClick = onDone
        ) {
            Text(
                text = stringResource(id = R.string.done_viewing_phrase),
                style = ButtonTextStyle
            )
        }
    }
}

//@Composable
//fun TimeleftForAccess() {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(color = SharedColors.BackgroundGrey)
//            .padding(vertical = 24.dp, horizontal = 24.dp),
//        horizontalArrangement = Arrangement.Center,
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Icon(
//            painterResource(id = R.drawable.time_left_icon),
//            contentDescription = "",
//            tint = SharedColors.MainIconColor
//        )
//        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
//
//        val accessTextStyle = SpanStyle(
//            fontSize = 16.sp,
//            color = SharedColors.MainColorText
//        )
//
//        val timeLeftText = buildAnnotatedString {
//            withStyle(accessTextStyle) {
//                append(stringResource(co.censo.censo.R.string.access_ends_in))
//            }
//            withStyle(accessTextStyle.copy(fontWeight = FontWeight.W600)) {
//                append(formatPhraseAccessDuration(timeLeft, context))
//            }
//        }
//
//        Text(
//            text = timeLeftText,
//            color = SharedColors.MainColorText
//        )
//    }
//}

fun formatPhraseAccessDuration(duration: Duration, context: Context) =
    if (duration.toInt(DurationUnit.MINUTES) in 1..14) {
        "${duration.toInt(DurationUnit.MINUTES) + 1} ${context.getString(co.censo.censo.R.string.min)}"
    } else {
        context.getString(co.censo.censo.R.string.less_than_1_minute)
    }

@Preview(name = "PIXEL", device = Devices.NEXUS_5, showSystemUi = true, showBackground = true)
@Composable
fun PreviewV15MinuteViewPhraseWordUI() {
    Box(modifier = Modifier.fillMaxSize()) {
        ViewAccessPhraseUI(
            phraseWords = listOf("lounge", "depart", "example"),
            onDone = {},
            timeLeft = 899.seconds
        )
    }
}

@Preview(name = "PIXEL", device = Devices.NEXUS_5, showSystemUi = true, showBackground = true)
@Composable
fun PreviewLess15ViewPhraseWordUI() {
    Box(modifier = Modifier.fillMaxSize()) {
        ViewAccessPhraseUI(
            phraseWords = listOf("lounge", "depart", "example"),
            onDone = {},
            timeLeft = 480.seconds
        )
    }
}

@Preview(name = "PIXEL", device = Devices.NEXUS_5, showSystemUi = true, showBackground = true)
@Composable
fun PreviewLess1ViewPhraseWordUI() {
    Box(modifier = Modifier.fillMaxSize()) {
        ViewAccessPhraseUI(
            phraseWords = listOf("lounge", "depart", "example"),
            onDone = {},
            timeLeft = 55.seconds
        )
    }
}