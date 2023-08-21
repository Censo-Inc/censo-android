package co.censo.vault.util

import android.util.Log
import co.censo.vault.BuildConfig

const val TAG = "WaterBottle"
fun vaultLog(tag: String = TAG, message: String) {
    if (BuildConfig.DEBUG) Log.d(tag, message)
}