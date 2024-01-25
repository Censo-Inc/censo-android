package co.censo.censo.presentation.login_id_reset

import co.censo.censo.presentation.entrance.ForceUserToGrantCloudStorageAccess
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GetOwnerUserApiResponse
import co.censo.shared.data.model.ResetLoginIdApiResponse

data class LoginIdResetState(
    val resetStep: LoginIdResetStep = LoginIdResetStep.PasteResetLinks,

    // step 1
    val collectedTokens: Int = 0,
    val requiredTokens: Int = 2,
    val linkError: Boolean = false,

    // step 2
    val triggerGoogleSignIn: Resource<Unit> = Resource.Uninitialized,
    val forceUserToGrantCloudStorageAccess: ForceUserToGrantCloudStorageAccess = ForceUserToGrantCloudStorageAccess(),
    val createDeviceResponse: Resource<Unit> = Resource.Uninitialized,

    // step 3
    val launchFacetec: Boolean = false,
    val resetLoginIdResponse: Resource<ResetLoginIdApiResponse> = Resource.Uninitialized,
    val userResponse: Resource<GetOwnerUserApiResponse> = Resource.Uninitialized,

    // step 4
    val navigationResource: Resource<String> = Resource.Uninitialized,
) {
    val isLoading = userResponse is Resource.Loading
            || resetLoginIdResponse is Resource.Loading
            || createDeviceResponse is Resource.Loading

    val apiCallErrorOccurred = userResponse is Resource.Error
            || resetLoginIdResponse is Resource.Error
            || triggerGoogleSignIn is Resource.Error
            || createDeviceResponse is Resource.Error
}

enum class LoginIdResetStep {
    PasteResetLinks,
    SelectLoginId,
    Facetec,
    TermsOfUse,
    KeyRecovery
}

sealed interface LoginIdResetAction {
    data class PasteLink(val clipboardContent: String?) : LoginIdResetAction
    data class TokenReceived(val token: String) : LoginIdResetAction
    data object SelectGoogleId : LoginIdResetAction
    data object CloudStoragePermissionsGranted : LoginIdResetAction
    data object Facescan : LoginIdResetAction
    data class TermsOfUseAccepted(val version: String) : LoginIdResetAction
    data object RetrieveUser : LoginIdResetAction
    data object KeyRecovery : LoginIdResetAction
    data object DetermineResetStep : LoginIdResetAction
    data object Retry : LoginIdResetAction
    data object Exit : LoginIdResetAction
}