package co.censo.vault.presentation.access_seed_phrases

import co.censo.shared.data.Resource
import co.censo.shared.data.model.RecoveredSeedPhrase
import co.censo.shared.data.model.RetrieveRecoveryShardsApiResponse


data class AccessSeedPhrasesScreenState(
    // data
    val recoveredPhrases: Resource<List<RecoveredSeedPhrase>> = Resource.Uninitialized,

    // api requests
    val retrieveShardsResponse: Resource<RetrieveRecoveryShardsApiResponse> = Resource.Uninitialized,

    // navigation
    val navigationResource: Resource<String> = Resource.Uninitialized,
) {

    val loading = retrieveShardsResponse is Resource.Loading
    val asyncError = retrieveShardsResponse is Resource.Error
}
