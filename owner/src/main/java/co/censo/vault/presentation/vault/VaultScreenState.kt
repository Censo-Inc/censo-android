package co.censo.vault.presentation.vault

import co.censo.shared.data.Resource
import co.censo.shared.data.model.DeleteSecretApiResponse
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.StoreSecretApiResponse

data class VaultScreenState(
    // owner state
    val ownerState: OwnerState.Ready? = null,

    // screen
    val screen: VaultScreens = VaultScreens.Unlocked,

    val triggerDeleteUserDialog: Resource<Unit> = Resource.Uninitialized,

    // api requests
    val userResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val storeSeedPhraseResource: Resource<StoreSecretApiResponse> = Resource.Uninitialized,
    val deleteSeedPhraseResource: Resource<DeleteSecretApiResponse> = Resource.Uninitialized,
    val deleteUserResource: Resource<Unit> = Resource.Uninitialized,

    // navigation
    val navigationResource: Resource<String> = Resource.Uninitialized,
) {

    val externalApprovers = (ownerState?.policy?.guardians?.size?.minus(1)) ?: 0
    val secretsSize = ownerState?.vault?.secrets?.size ?: 0

    val loading = userResponse is Resource.Loading ||
            storeSeedPhraseResource is Resource.Loading ||
            deleteSeedPhraseResource is Resource.Loading ||
            deleteUserResource is Resource.Loading

    val asyncError =
        userResponse is Resource.Error ||
                storeSeedPhraseResource is Resource.Error ||
                deleteSeedPhraseResource is Resource.Error ||
                deleteUserResource is Resource.Error

}