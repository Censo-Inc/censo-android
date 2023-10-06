package co.censo.vault.presentation.lock_screen

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    Text(
        modifier = Modifier.padding(6.dp),
        text = "Locks in $secondsLeft seconds",
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.W300,
    )
}
private fun secondsUntil(locksAt: Instant): Long {
    return locksAt.minus(Clock.System.now()).inWholeSeconds
}