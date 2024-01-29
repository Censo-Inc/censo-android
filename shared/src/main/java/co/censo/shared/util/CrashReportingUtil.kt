package co.censo.shared.util

import io.sentry.Sentry

object CrashReportingUtil {
    //Keys
    const val ERROR_MESSAGE_KEY = "ErrorMessage"
    const val MANUALLY_REPORTED_TAG = "ManualReport"

    //Values
    const val AccessConfirmation = "AccessConfirmation"
    const val AuthRestConfirmation = "AuthConfirmation"
    const val CloudUpload = "CloudUpload"
    const val CloudDownload = "CloudDownload"
    const val SubmitVerification = "SubmitVerification"
    const val PastePhrase = "PastePhrase"
    const val CreateApproverKey = "CreateApproverKey"
    const val InviteDeeplink = "InviteDeeplink"
    const val AuthHeaders = "AuthHeaders"
    const val TotpVerification = "TotpVerification"
    const val ReplacePolicy = "ReplacePolicy"
    const val ReplacePolicyShards = "ReplacePolicyShards"
    const val JWTToken = "JWTToken"
    const val EncryptShard = "EncryptShard"
    const val DecryptShard = "DecryptShard"
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
    const val LoginIdReset = "LoginIdReset"
    const val SubmitNotificationToken = "SubmitNotificationToken"
    const val SilentRefreshToken = "SilentRefreshToken"
    const val RetrieveAccount = "RetrieveAccount"
    const val BillingSubscription = "BillingSubscription"
    const val AccessPhrase = "AccessPhrase"
    const val PlayIntegrity = "PlayIntegrity"
    const val VerifyKeyConfirmation = "VerifyKeyConfirmation"
    const val DecryptingKey = "DecryptingKey"
    const val ImportPhrase = "ImportPhrase"
    const val ImageCapture = "PhotoCapture"
    const val ImageReview = "ImageReview"
}

fun Exception.sendError(reason: String) {
    Sentry.captureException(this)
}
