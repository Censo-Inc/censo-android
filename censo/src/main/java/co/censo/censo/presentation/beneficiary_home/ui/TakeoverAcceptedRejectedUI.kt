package co.censo.censo.presentation.beneficiary_home.ui

import StandardButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.days

@Composable
fun TakeoverAcceptedRejectedUI(
    accepted: Boolean,
    approverLabel: String,
    countdownTime: Instant?,
    onCancelTakeover: () -> Unit
) {
    val basicStyle = SpanStyle(
        color = SharedColors.MainColorText,
        fontSize = 16.sp
    )

    val annotatedMessage = buildAnnotatedString {

        if (accepted) {
            withStyle(basicStyle) {
                append(
                    stringResource(R.string.takeover_initiation_part_one, approverLabel)
                )
            }

            withStyle(basicStyle.copy(fontWeight = FontWeight.W700)) {
                append(" ")
                append(countdownTime?.timeLeft() ?: stringResource(R.string.some_time))
                append(".\n\n")
            }

            withStyle(basicStyle) {
                append(
                    stringResource(id = R.string.takeover_initiation_part_two, approverLabel),
                )
            }
        } else {
            append(stringResource(R.string.takeover_rejected, approverLabel))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = annotatedMessage,
            color = SharedColors.MainColorText,
            fontSize = 16.sp
        )

        StandardButton(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp),
            onClick = onCancelTakeover,
        ) {
            Text(
                text = stringResource(R.string.cancel_takeover),
                style = ButtonTextStyle.copy(fontSize = 20.sp)
            )
        }

    }
}

fun Instant.timeLeft(): String {
    val currentInstant = Clock.System.now()
    val durationInSeconds = (this - currentInstant).inWholeSeconds.absoluteValue
    val days = durationInSeconds / (24 * 3600)
    val hours = (durationInSeconds % (24 * 3600)) / 3600
    val minutes = (durationInSeconds % 3600) / 60

    return "$days days, $hours hours, $minutes minutes"
}


@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewTakeoverAcceptedUI() {
    TakeoverAcceptedRejectedUI(
        accepted = true,
        approverLabel = "Jason",
        countdownTime = Clock.System.now().plus(3.days)
    ) {

    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewTakeoverRejectedUI() {
    TakeoverAcceptedRejectedUI(
        accepted = false,
        approverLabel = "Jason",
        countdownTime = null
    ) {

    }
}