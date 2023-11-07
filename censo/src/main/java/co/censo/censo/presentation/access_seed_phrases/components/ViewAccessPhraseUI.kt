package co.censo.censo.presentation.access_seed_phrases.components

import StandardButton
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.R
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

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Column {


            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = SharedColors.BackgroundGrey)
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Row(
                        modifier = Modifier.weight(0.70f),
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
                                append("Access ends in ")
                            }
                            withStyle(accessTextStyle.copy(fontWeight = FontWeight.W600)) {
                                append(formatPhraseAccessDuration(timeLeft))
                            }
                        }

                        Text(
                            text = timeLeftText,
                            color = Color.Black
                        )
                    }

                    Box(
                        modifier = Modifier.weight(0.30f),
                        contentAlignment = Alignment.Center,
                    ) {
                        StandardButton(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            onClick = onDone
                        ) {
                            Text(
                                text = "Done",
                                color = Color.White
                            )
                        }
                    }
                }
            }
            Divider()
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
                    painter = painterResource(R.drawable.arrow_right),
                    contentDescription = stringResource(id = co.censo.censo.R.string.move_one_word_back),
                    tint = Color.Black
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Divider(
                color = SharedColors.DividerGray,
                thickness = 0.75.dp
            )
            Spacer(modifier = Modifier.height(28.dp))
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
                        .weight(0.20f)
                        .padding(8.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(44.dp),
                        painter = painterResource(id = co.censo.censo.R.drawable.warning),
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

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
                        fontSize = 13.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

fun formatPhraseAccessDuration(duration: Duration): String {
    val minutes = duration.toInt(DurationUnit.MINUTES)
    val seconds = duration.minus(minutes.minutes).inWholeSeconds

    val formattedMinutes = "${if (minutes < 10) "0$minutes" else "$minutes"} min"
    val formattedSeconds = "${if (seconds < 10) "0$seconds" else "$seconds"} sec."

    return "$formattedMinutes $formattedSeconds".trim()
}

@Preview(name = "PIXEL", device = Devices.NEXUS_5, showSystemUi = true, showBackground = true)
@Composable
fun PreviewViewPhraseWordUI() {
    Box(modifier = Modifier.fillMaxSize()) {
        ViewAccessPhraseUI(
            wordIndex = 5,
            phraseWord = "lounge",
            decrementIndex = { },
            incrementIndex = { },
            onDone = {},
            timeLeft = 1199.seconds
        )
    }
}