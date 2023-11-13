package co.censo.censo.presentation.access_seed_phrases.components

import StandardButton
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import co.censo.shared.R as SharedR
import co.censo.shared.presentation.SharedColors
import co.censo.censo.presentation.enter_phrase.components.ViewPhraseWord
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@Composable
fun ViewAccessPhraseUI(
    wordIndex: Int,
    phraseWord: String,
    decrementIndex: () -> Unit,
    incrementIndex: () -> Unit,
    onDone: () -> Unit,
    timeLeft: Duration
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = SharedColors.BackgroundGrey)
                .padding(vertical = 24.dp, horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(id = co.censo.censo.R.drawable.time_left_icon),
                contentDescription = "",
            )
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))

            val accessTextStyle = SpanStyle(
                fontSize = 16.sp,
                color = Color.Black
            )

            val timeLeftText = buildAnnotatedString {
                withStyle(accessTextStyle) {
                    append(stringResource(co.censo.censo.R.string.access_ends_in))
                }
                withStyle(accessTextStyle.copy(fontWeight = FontWeight.W600)) {
                    append(formatPhraseAccessDuration(timeLeft, context))
                }
            }

            Text(
                text = timeLeftText,
                color = Color.Black
            )
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
                    painter = painterResource(SharedR.drawable.arrow_left),
                    contentDescription = stringResource(id = co.censo.censo.R.string.move_one_word_back),
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
                    painter = painterResource(SharedR.drawable.arrow_right),
                    contentDescription = stringResource(id = co.censo.censo.R.string.move_one_word_back),
                    tint = Color.Black
                )
            }
        }

        StandardButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp, end = 56.dp, top = 12.dp, bottom = 24.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            onClick = onDone
        ) {
            Text(
                text = stringResource(id = co.censo.censo.R.string.done),
                color = Color.White,
                fontSize = 20.sp
            )
        }
    }
}

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
            wordIndex = 5,
            phraseWord = "lounge",
            decrementIndex = { },
            incrementIndex = { },
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
            wordIndex = 5,
            phraseWord = "lounge",
            decrementIndex = { },
            incrementIndex = { },
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
            wordIndex = 5,
            phraseWord = "lounge",
            decrementIndex = { },
            incrementIndex = { },
            onDone = {},
            timeLeft = 55.seconds
        )
    }
}