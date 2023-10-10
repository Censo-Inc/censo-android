package co.censo.shared.presentation.entrance

import co.censo.shared.data.Resource
import okhttp3.ResponseBody

data class EntranceState(
    val triggerGoogleSignIn: Resource<Unit> = Resource.Uninitialized,
    val createUserResource: Resource<ResponseBody> = Resource.Uninitialized,
    val showPushNotificationsDialog: Resource<Unit> = Resource.Uninitialized,
    val userFinishedSetup: Resource<String> = Resource.Uninitialized,
    val authId: String = ""
) {
    val isLoading = createUserResource is Resource.Loading

    val apiCallErrorOccurred =
        createUserResource is Resource.Error || triggerGoogleSignIn is Resource.Error
}