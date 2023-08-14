package co.censo.vault

import android.util.Log

const val TAG = "WaterBottle"
fun vaultLog(tag: String = TAG, message: String) {
    if (BuildConfig.DEBUG) Log.d(tag, message)
}