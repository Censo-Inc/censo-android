package co.censo.shared.util

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context

object ClipboardHelper {

    fun getClipboardContent(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        // Check if the clipboard has data
        if (clipboard.hasPrimaryClip() && clipboard.primaryClipDescription?.hasMimeType(
                ClipDescription.MIMETYPE_TEXT_PLAIN) == true) {
            val item = clipboard.primaryClip?.getItemAt(0)
            return item?.text?.toString()
        }
        return null
    }
}