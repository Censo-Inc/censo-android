package co.censo.vault.presentation.owner_entrance

import co.censo.shared.data.Resource
import okhttp3.ResponseBody

data class OwnerEntranceState(
    val triggerOneTap: Resource<Unit> = Resource.Uninitialized,
    val createUserResource: Resource<ResponseBody> = Resource.Uninitialized,
    val userFinishedSetup: Resource<String> = Resource.Uninitialized,
    val authId: String = ""
) {
    val isLoading = createUserResource is Resource.Loading

    val apiCallErrorOccurred =
        createUserResource is Resource.Error || triggerOneTap is Resource.Error
}