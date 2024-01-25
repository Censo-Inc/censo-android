package co.censo.censo.presentation.access_seed_phrases.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.censo.R
import co.censo.shared.presentation.SharedColors
import kotlin.time.Duration
import kotlin.time.DurationUnit

@Composable
fun TimeLeftForAccess(
    timeLeft: Duration,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = SharedColors.BackgroundGrey)
            .padding(vertical = 24.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(id = R.drawable.time_left_icon),
            contentDescription = "",
            tint = SharedColors.MainIconColor
        )
        Spacer(modifier = Modifier.padding(horizontal = 4.dp))

        val accessTextStyle = SpanStyle(
            fontSize = 16.sp,
            color = SharedColors.MainColorText
        )

        val timeLeftText = buildAnnotatedString {
            withStyle(accessTextStyle) {
                append(stringResource(R.string.access_ends_in))
            }
            withStyle(accessTextStyle.copy(fontWeight = FontWeight.W600)) {
                append(formatPhraseAccessDuration(timeLeft, context))
            }
        }

        Text(
            text = timeLeftText,
            color = SharedColors.MainColorText
        )
    }
}

fun formatPhraseAccessDuration(duration: Duration, context: Context) =
    if (duration.toInt(DurationUnit.MINUTES) in 1..14) {
        "${duration.toInt(DurationUnit.MINUTES) + 1} ${context.getString(co.censo.censo.R.string.min)}"
    } else {
        context.getString(co.censo.censo.R.string.less_than_1_minute)
    }