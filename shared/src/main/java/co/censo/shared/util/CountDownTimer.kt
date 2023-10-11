package co.censo.shared.util

import android.os.CountDownTimer


interface VaultCountDownTimer {
    fun startCountDownTimer(countdownInterval: Long, onTickCallback: () -> Unit)
    fun stopCountDownTimer()
}

class CountDownTimerImpl : VaultCountDownTimer {
    private var timer: CountDownTimer? = null

    override fun startCountDownTimer(countdownInterval: Long, onTickCallback: () -> Unit) {
        stopCountDownTimer()
        timer = object : CountDownTimer(Long.MAX_VALUE, countdownInterval) {
            override fun onTick(millisUntilFinished: Long) {
                onTickCallback()
            }

            override fun onFinish() {}
        }
        timer?.start()
    }

    override fun stopCountDownTimer() {
        timer?.cancel()
    }

    object Companion {
        const val UPDATE_COUNTDOWN = 1_000L
        const val POLLING_VERIFICATION_COUNTDOWN = 5_000L
    }
}