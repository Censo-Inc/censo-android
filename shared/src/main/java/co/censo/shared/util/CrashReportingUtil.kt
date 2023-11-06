package co.censo.shared.util

import co.censo.shared.util.CrashReportingUtil.ERROR_MESSAGE_KEY
import co.censo.shared.util.CrashReportingUtil.MANUALLY_REPORTED_TAG
import com.raygun.raygun4android.RaygunClient

object CrashReportingUtil {
    const val RecoveryConfirmation = "RecoveryConfirmation"
    const val CLOUD_UPLOAD = "CloudUpload"
    const val CLOUD_DOWNLOAD = "CloudDownload"
    const val ERROR_MESSAGE_KEY = "ErrorMessage"
    const val MANUALLY_REPORTED_TAG = "ManualReport"

}

fun Exception.sendError(reason: String, origin: String? = null) {
    val errorMessageData = mapOf(ERROR_MESSAGE_KEY to reason)

    val tagList = mutableListOf(MANUALLY_REPORTED_TAG)
    if (origin != null) { tagList.add(origin) }

    RaygunClient.send(this, tagList, errorMessageData)
}