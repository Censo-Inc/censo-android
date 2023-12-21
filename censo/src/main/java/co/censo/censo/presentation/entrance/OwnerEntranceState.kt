package co.censo.censo.presentation.entrance

import co.censo.censo.util.NavigationData
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GetOwnerUserApiResponse
import okhttp3.ResponseBody

data class OwnerEntranceState(
    val triggerGoogleSignIn: Resource<Unit> = Resource.Uninitialized,
    val signInUserResource: Resource<ResponseBody> = Resource.Loading(),
    val authId: String = "",
    val forceUserToGrantCloudStorageAccess: ForceUserToGrantCloudStorageAccess = ForceUserToGrantCloudStorageAccess(),
    val acceptedTermsOfUseVersion: String? = null,
    val showAcceptTermsOfUse: Boolean = false,
    val userFinishedSetup: Boolean = false,

    val userResponse: Resource<GetOwnerUserApiResponse> = Resource.Uninitialized,
    val navigationResource: Resource<NavigationData> = Resource.Uninitialized,
) {
    val isLoading = signInUserResource is Resource.Loading
            || userResponse is Resource.Loading

    val apiCallErrorOccurred =  signInUserResource is Resource.Error
            || triggerGoogleSignIn is Resource.Error
            || userResponse is Resource.Error
}

data class ForceUserToGrantCloudStorageAccess(
    val requestAccess: Boolean = false,
    val jwt: String? = ""
)