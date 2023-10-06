package co.censo.vault.presentation.vault

import co.censo.shared.data.Resource
import co.censo.shared.data.model.DeleteSecretApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.StoreSecretApiResponse

data class VaultHomeState(
    val ownerStateResource: Resource<OwnerState> = Resource.Uninitialized,
    val storeSeedPhraseResource: Resource<StoreSecretApiResponse> = Resource.Uninitialized,
    val deleteSeedPhraseResource: Resource<DeleteSecretApiResponse> = Resource.Uninitialized
) {

    val loading = ownerStateResource is Resource.Loading ||
            storeSeedPhraseResource is Resource.Loading ||
            deleteSeedPhraseResource is Resource.Loading

    val asyncError =
        ownerStateResource is Resource.Error ||
                storeSeedPhraseResource is Resource.Error ||
                deleteSeedPhraseResource is Resource.Error

}