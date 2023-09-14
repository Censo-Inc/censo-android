package co.censo.vault.presentation.owner_entrance

import co.censo.shared.data.Resource
import co.censo.shared.data.model.GetUserApiResponse
import okhttp3.ResponseBody

data class OwnerEntranceState(
    val createOwnerResource: Resource<ResponseBody> = Resource.Uninitialized,
    val userResource: Resource<GetUserApiResponse?> = Resource.Uninitialized,
    val userStatus: UserStatus = UserStatus.UNINITIALIZED,
    val validationError: String = "",
    val verificationId: String = "",
    val bioPromptTrigger: Resource<Unit> = Resource.Uninitialized,
    val userFinishedSetup: Resource<String> = Resource.Uninitialized
) {
    val isLoading =
        createOwnerResource is Resource.Loading || userResource is Resource.Loading

    val apiCallErrorOccurred =
        createOwnerResource is Resource.Error || userResource is Resource.Error
}

enum class UserStatus {
    UNINITIALIZED
}