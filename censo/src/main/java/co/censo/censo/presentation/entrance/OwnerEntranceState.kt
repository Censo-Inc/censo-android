package co.censo.censo.presentation.entrance

import co.censo.shared.data.Resource
import co.censo.shared.data.model.GetOwnerUserApiResponse
import co.censo.shared.util.NavigationData

data class OwnerEntranceState(
    val triggerGoogleSignIn: Resource<Unit> = Resource.Uninitialized,
    val signInUserResource: Resource<Unit> = Resource.Loading,
    val forceUserToGrantCloudStorageAccess: ForceUserToGrantCloudStorageAccess = ForceUserToGrantCloudStorageAccess(),
    val acceptedTermsOfUseVersion: String? = null,
    val showAcceptTermsOfUse: Boolean = false,
    val userFinishedSetup: Boolean = false,
    val userIsOnboarding: Boolean = false,

    val userResponse: Resource<GetOwnerUserApiResponse> = Resource.Uninitialized,
    val deleteUserResource: Resource<Unit> = Resource.Uninitialized,
    val navigationResource: Resource<NavigationData> = Resource.Uninitialized,
    val triggerDeleteUserDialog: Resource<Unit> = Resource.Uninitialized,
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