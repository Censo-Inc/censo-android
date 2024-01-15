package co.censo.censo.presentation.login_id_reset
import co.censo.shared.data.Resource
import co.censo.shared.data.model.ResetLoginIdApiResponse

data class LoginIdResetState(
    val resetStep: LoginIdResetStep = LoginIdResetStep.PasteResetLinks,

    // step 1
    val collectedTokens: Int = 0,
    val requiredTokens: Int = 2,
    val linkError: Boolean = false,

    // step 2
    val triggerGoogleSignIn: Resource<Unit> = Resource.Uninitialized,
    val idToken: String = "",
    val createDeviceResponse: Resource<Unit> = Resource.Uninitialized,

    // step 3
    val launchFacetec: Boolean = false,
    val resetLoginIdResponse: Resource<ResetLoginIdApiResponse> = Resource.Uninitialized,

    // step 4
    val navigationResource: Resource<String> = Resource.Uninitialized,
) {
    val isLoading = resetLoginIdResponse is Resource.Loading
            || createDeviceResponse is Resource.Loading

    val apiCallErrorOccurred =  resetLoginIdResponse is Resource.Error
            || triggerGoogleSignIn is Resource.Error
            || createDeviceResponse is Resource.Error
}

enum class LoginIdResetStep {
    PasteResetLinks,
    SelectLoginId,
    Facetec,
    KeyRecovery
}

sealed interface LoginIdResetAction {
    data class PasteLink(val clipboardContent: String?) : LoginIdResetAction
    data class TokenReceived(val token: String) : LoginIdResetAction
    object SelectGoogleId : LoginIdResetAction
    object Facescan : LoginIdResetAction
    object KeyRecovery : LoginIdResetAction
    object DetermineResetStep : LoginIdResetAction
    object Retry : LoginIdResetAction
    object Exit : LoginIdResetAction
}