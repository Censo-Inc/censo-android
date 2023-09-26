package co.censo.vault.presentation.owner_ready

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.time.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Composable
fun LockCountDown(locksAt: Instant, onTimeOut: () -> Unit) {

    var secondsLeft by remember { mutableStateOf(secondsUntil(locksAt)) }

    LaunchedEffect(key1 = secondsLeft) {
        if (secondsLeft > 0) {
            delay(1.seconds.toJavaDuration())
            secondsLeft = secondsUntil(locksAt)
        } else {
            onTimeOut()
        }
    }

    Text(text = "Locks in $secondsLeft seconds", fontSize = 12.sp)
}
private fun secondsUntil(locksAt: Instant): Long {
    return locksAt.minus(Clock.System.now()).inWholeSeconds
}