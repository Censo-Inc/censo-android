package co.censo.shared.util

import co.censo.shared.util.CrashReportingUtil.ERROR_MESSAGE_KEY
import co.censo.shared.util.CrashReportingUtil.MANUALLY_REPORTED_TAG
import com.raygun.raygun4android.RaygunClient

object CrashReportingUtil {
    //Keys
    const val ERROR_MESSAGE_KEY = "ErrorMessage"
    const val MANUALLY_REPORTED_TAG = "ManualReport"

    //Values
    const val RecoveryConfirmation = "RecoveryConfirmation"
    const val CloudUpload = "CloudUpload"
    const val CloudDownload = "CloudDownload"
    const val SubmitVerification = "SubmitVerification"
    const val PastePhrase = "PastePhrase"
    const val CreateApproverKey = "CreateApproverKey"
    const val InviteDeeplink = "InviteDeeplink"
    const val AuthHeaders = "AuthHeaders"
    const val TotpVerification = "TotpVerification"
    const val ReplacePolicy = "ReplacePolicy"
    const val JWTToken = "JWTToken"
    const val EncryptShard = "EncryptShard"
    const val DeleteUser = "DeleteUser"
    const val RemovePushNotification = "RemovePushNotification"
    const val RetrieveFileContent = "RetrieveFileContent"
    const val DeleteFile = "DeleteFile"
    const val RetrieveDriveService = "RetrieveDriveService"
    const val CloudStorageIntent = "CloudStorageIntent"
    const val PermissionDialog = "PermissionDialog"
    const val SignIn = "SignIn"
    const val SignOut = "SignOut"
    const val SubmitNotificationToken = "SubmitNotificationToken"
    const val SilentRefreshToken = "SilentRefreshToken"
    const val RetrieveAccount = "RetrieveAccount"
}

fun Exception.sendError(reason: String, origin: String? = null) {
    val errorMessageData = mapOf(ERROR_MESSAGE_KEY to reason)

    val tagList = mutableListOf(MANUALLY_REPORTED_TAG)
    if (origin != null) { tagList.add(origin) }

    RaygunClient.send(this, tagList, errorMessageData)
}