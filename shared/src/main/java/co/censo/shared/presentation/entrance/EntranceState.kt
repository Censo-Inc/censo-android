package co.censo.shared.presentation.entrance

import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState
import okhttp3.ResponseBody

data class EntranceState(
    val triggerGoogleSignIn: Resource<Unit> = Resource.Uninitialized,
    val signInUserResource: Resource<ResponseBody> = Resource.Loading(),
    val showPushNotificationsDialog: Resource<Unit> = Resource.Uninitialized,
    val userFinishedSetup: Resource<String> = Resource.Uninitialized,
    val authId: String = "",
    val forceUserToGrantCloudStorageAccess: ForceUserToGrantCloudStorageAccess = ForceUserToGrantCloudStorageAccess(),
    val acceptedTermsOfUseVersion: String? = null,
    val showAcceptTermsOfUse: Boolean = false
) {
    val isLoading = signInUserResource is Resource.Loading
    val apiCallErrorOccurred =  signInUserResource is Resource.Error || triggerGoogleSignIn is Resource.Error
}

data class ForceUserToGrantCloudStorageAccess(
    val requestAccess: Boolean = false,
    val jwt: String? = ""
)