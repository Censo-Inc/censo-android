package co.censo.shared.util

import android.os.CountDownTimer


interface VaultCountDownTimer {
    fun start(interval: Long, skipFirstTick: Boolean = false, onTickCallback: () -> Unit)
    fun stop()
    fun stopWithDelay(delay: Long)
}

class CountDownTimerImpl : VaultCountDownTimer {
    private var firstTickSkipped: Boolean = false
    private var timer: CountDownTimer? = null
    private var delayedStopTimer: CountDownTimer? = null

    override fun start(interval: Long, skipFirstTick: Boolean, onTickCallback: () -> Unit) {
        stop()
        timer = object : CountDownTimer(Long.MAX_VALUE, interval) {
            override fun onTick(millisUntilFinished: Long) {
                if (skipFirstTick && !firstTickSkipped) {
                    firstTickSkipped = true
                    return
                }

                onTickCallback()
            }

            override fun onFinish() {}
        }
        timer?.start()
    }

    override fun stop() {
        delayedStopTimer?.cancel()
        timer?.cancel()
        timer = null
    }

    override fun stopWithDelay(delay: Long) {
        timer?.let {
            delayedStopTimer?.cancel()
            delayedStopTimer = object : CountDownTimer(delay, delay) {
                override fun onTick(millisUntilFinished: Long) {}

                override fun onFinish() {
                    stop()
                }
            }
            delayedStopTimer?.start()
        }
    }

    object Companion {
        const val UPDATE_COUNTDOWN = 1_000L
        const val POLLING_VERIFICATION_COUNTDOWN = 3_000L
        const val VERIFICATION_STOP_DELAY = 30_000L
        const val MAINTENANCE_MODE_COUNTDOWN = 3_000L
    }
}