package co.censo.vault.presentation.vault

import co.censo.shared.data.Resource
import co.censo.shared.data.model.DeleteSecretApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.StoreSecretApiResponse

data class VaultState(
    val ownerState: OwnerState.Ready? = null,

    val storeSeedPhraseResource: Resource<StoreSecretApiResponse> = Resource.Uninitialized,
    val deleteSeedPhraseResource: Resource<DeleteSecretApiResponse> = Resource.Uninitialized
) {

    val loading = ownerState == null ||
            storeSeedPhraseResource is Resource.Loading ||
            deleteSeedPhraseResource is Resource.Loading

    val asyncError = storeSeedPhraseResource is Resource.Error ||
            deleteSeedPhraseResource is Resource.Error

}