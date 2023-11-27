package co.censo.shared.util

import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper


interface VaultCountDownTimer {
    fun start(interval: Long, onTickCallback: () -> Unit)
    fun startWithDelay(initialDelay: Long, interval: Long, onTickCallback: () -> Unit)
    fun stop()
}

class CountDownTimerImpl : VaultCountDownTimer {
    private var timer: CountDownTimer? = null
    private var cancelRequested = false
    override fun startWithDelay(
        initialDelay: Long,
        interval: Long,
        onTickCallback: () -> Unit
    ) {
        Handler(Looper.getMainLooper()).postDelayed(
            { start(interval, onTickCallback) },
            initialDelay
        )
    }

    override fun start(interval: Long, onTickCallback: () -> Unit) {
        stop()
        timer = object : CountDownTimer(Long.MAX_VALUE, interval) {
            override fun onTick(millisUntilFinished: Long) {
                if (cancelRequested) {
                    this.cancel()
                } else {
                    onTickCallback()
                }
            }

            override fun onFinish() {}
        }
        timer?.start()
    }

    override fun stop() {
        // timer might be not started yet and hence be null. This may in case when owner view receives
        // on_stop lifecycle event while its init coroutine still running.
        // Therefore scheduling cancellation on the next timer tick.
        cancelRequested = true

        // and attempt to cancel immediately
        timer?.cancel()
    }

    object Companion {
        const val INITIAL_DELAY = 2_000L
        const val UPDATE_COUNTDOWN = 1_000L
        const val POLLING_VERIFICATION_COUNTDOWN = 3_000L
    }
}