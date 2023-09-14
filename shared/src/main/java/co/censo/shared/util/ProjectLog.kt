package co.censo.shared.util

import android.util.Log
import co.censo.shared.BuildConfig

const val TAG = "StickyLog"
fun log(tag: String = TAG, message: String) {
    if (BuildConfig.DEBUG) Log.d(tag, message)
}