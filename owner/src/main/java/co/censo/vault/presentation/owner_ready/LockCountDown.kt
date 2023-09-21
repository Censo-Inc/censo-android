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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Composable
fun LockCountDown(timeLeft: Duration, onTimeOut: () -> Unit) {
    var secondsLeft by remember { mutableStateOf(timeLeft.inWholeSeconds) }

    LaunchedEffect(key1 = secondsLeft) {
        if (secondsLeft > 0) {
            delay(1.seconds.toJavaDuration())
            secondsLeft -= 1
        } else {
            onTimeOut()
        }
    }

    Text(text = "Locks in $secondsLeft seconds", fontSize = 12.sp)
}