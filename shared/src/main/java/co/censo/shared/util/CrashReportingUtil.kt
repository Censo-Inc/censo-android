package co.censo.shared.util

import io.sentry.Sentry

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
    const val PushNotification = "PushNotification"
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
    const val BillingSubscription = "BillingSubscription"
    const val AccessPhrase = "AccessPhrase"
}

fun Exception.sendError(reason: String) {
    Sentry.captureException(this)
}