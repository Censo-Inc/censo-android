package co.censo.vault.presentation.components.recovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.vault.R
import co.censo.vault.presentation.VaultColors
import kotlinx.coroutines.time.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toJavaDuration

@Composable
fun RecoveryExpirationCountDown(
    expiresAt: Instant,
    onTimeOut: () -> Unit
) {

    var durationLeft by remember { mutableStateOf(durationUnit(expiresAt)) }

    LaunchedEffect(key1 = durationLeft) {
        if (durationLeft.inWholeSeconds > 0) {
            delay(1.seconds.toJavaDuration())
            durationLeft = durationUnit(expiresAt)
        } else {
            onTimeOut()
        }
    }

    Text(
        modifier = Modifier.padding(6.dp),
        text = stringResource(R.string.expires_in_placeholder, formatDuration(durationLeft)),
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.W300,
    )
}

fun formatDuration(duration: Duration): String {
    val days: Long = duration.inWholeDays
    val hours = duration.minus(days.days).toInt(DurationUnit.HOURS)
    val minutes = duration.minus(days.days + hours.hours).toInt(DurationUnit.MINUTES)
    val seconds = duration.minus(days.days + hours.hours + minutes.minutes).inWholeSeconds

    val formattedDays = if (days > 0) "${days}d " else ""
    val formattedHours = if (hours > 0) "${hours}h " else ""
    val formattedMinutes = if (minutes > 0) "${minutes}m " else ""
    val formattedSeconds = if (seconds > 0) "${seconds}s" else ""

    return "$formattedDays$formattedHours$formattedMinutes$formattedSeconds".trim()
}

private fun durationUnit(expiresAt: Instant): Duration {
    return expiresAt.minus(Clock.System.now())
}

@Preview
@Composable
fun RecoveryExpirationCountDownPreview() {
    Box(modifier = Modifier.background(color = VaultColors.PrimaryColor)) {
        RecoveryExpirationCountDown(
            expiresAt = Clock.System.now() + 2.days + 3.hours + 25.minutes + 5.seconds,
            onTimeOut = {}
        )
    }
}