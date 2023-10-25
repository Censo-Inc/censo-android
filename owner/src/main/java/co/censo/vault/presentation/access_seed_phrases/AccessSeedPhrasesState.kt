package co.censo.vault.presentation.access_seed_phrases

import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.RecoveredSeedPhrase
import co.censo.shared.data.model.RetrieveRecoveryShardsApiResponse
import co.censo.shared.data.model.VaultSecret


data class AccessSeedPhrasesState(

    //UI state
    val accessPhrasesUIState: AccessPhrasesUIState = AccessPhrasesUIState.SelectPhrase,

    // data
    val recoveredPhrases: Resource<List<RecoveredSeedPhrase>> = Resource.Uninitialized,
    val ownerState: Resource<OwnerState> = Resource.Uninitialized,
    val selectedPhrase : VaultSecret? = null,

    // api requests
    val retrieveShardsResponse: Resource<RetrieveRecoveryShardsApiResponse> = Resource.Uninitialized,

    // navigation
    val navigationResource: Resource<String> = Resource.Uninitialized,
) {

    val loading = retrieveShardsResponse is Resource.Loading
                || ownerState is Resource.Loading
                || recoveredPhrases is Resource.Loading

    val asyncError = retrieveShardsResponse is Resource.Error
            || recoveredPhrases is Resource.Error || ownerState is Resource.Error

}

enum class AccessPhrasesUIState {
    SelectPhrase, ReadyToStart, Facetec, ViewPhrase
}
