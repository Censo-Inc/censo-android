package co.censo.shared.util

import android.content.ClipboardManager
import android.content.Context

object ClipboardHelper {

    fun getClipboardContent(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        if (!clipboard.hasPrimaryClip()) {
            return null
        }

        return clipboard.primaryClip?.getItemAt(0)?.coerceToText(context).toString()
    }

    fun clearClipboardContent(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        if (clipboard.hasPrimaryClip()) {
            clipboard.clearPrimaryClip()
        }
    }
}