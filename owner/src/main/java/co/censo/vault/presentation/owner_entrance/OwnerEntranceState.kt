package co.censo.vault.presentation.owner_entrance

import co.censo.shared.data.Resource
import okhttp3.ResponseBody

data class OwnerEntranceState(
    val createOwnerResource: Resource<ResponseBody> = Resource.Uninitialized,
    val userFinishedSetup: Resource<String> = Resource.Uninitialized,
    val authId: String = ""
) {
    val isLoading = createOwnerResource is Resource.Loading

    val apiCallErrorOccurred = createOwnerResource is Resource.Error
}