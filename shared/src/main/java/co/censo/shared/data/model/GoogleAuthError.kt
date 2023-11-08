package co.censo.shared.data.model

sealed class GoogleAuthError(val exception: Exception) {
    object InvalidToken : GoogleAuthError(Exception("Invalid Token"))
    object MissingCredentialId : GoogleAuthError(Exception("Missing Google Credential Id"))
    object UserCanceledGoogleSignIn : GoogleAuthError(Exception("User Canceled Google Auth"))
    object IntentResultFailed : GoogleAuthError(Exception("Intent Result Failed"))
    data class ErrorParsingIntent(val e: Exception) : GoogleAuthError(e)
    data class FailedToSignUserOut(val e: Exception) : GoogleAuthError(e)
    data class FailedToLaunchGoogleAuthUI(val e: Exception) : GoogleAuthError(e)
    data class FailedToVerifyId(val e: Exception) : GoogleAuthError(e)
    data class FailedToCreateKeyWithId(val e: Exception) : GoogleAuthError(e)
}