package co.censo.censo.presentation.access_seed_phrases

import SeedPhraseId
import co.censo.shared.data.Resource
import co.censo.shared.data.model.DeleteAccessApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.RecoveredSeedPhrase
import co.censo.shared.data.model.RetrieveAccessShardsApiResponse
import co.censo.shared.data.model.SeedPhrase
import co.censo.shared.util.BIP39
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


data class AccessSeedPhrasesState(

    //UI state
    val accessPhrasesUIState: AccessPhrasesUIState = AccessPhrasesUIState.SelectPhrase,
    val timeRemaining: Duration = 0.seconds,
    val locksAt: Instant? = null,
    val viewedPhrase: List<String> = emptyList(),
    val showCancelConfirmationDialog: Boolean = false,
    val viewedPhraseIds: List<SeedPhraseId> = emptyList(),

    // data
    val recoveredPhrases: Resource<List<RecoveredSeedPhrase>> = Resource.Uninitialized,
    val ownerState: Resource<OwnerState> = Resource.Uninitialized,
    val selectedPhrase : SeedPhrase? = null,
    val currentLanguage: BIP39.WordListLanguage? = null,

    // api requests
    val retrieveShardsResponse: Resource<RetrieveAccessShardsApiResponse> = Resource.Uninitialized,
    val cancelAccessResource: Resource<DeleteAccessApiResponse> = Resource.Uninitialized,

    // navigation
    val navigationResource: Resource<String> = Resource.Uninitialized,
) {

    val loading = retrieveShardsResponse is Resource.Loading
                || ownerState is Resource.Loading
                || cancelAccessResource is Resource.Loading
                || recoveredPhrases is Resource.Loading

    val asyncError = retrieveShardsResponse is Resource.Error
            || recoveredPhrases is Resource.Error
            || ownerState is Resource.Error
            || cancelAccessResource is Resource.Error
}

enum class AccessPhrasesUIState {
    SelectPhrase, ReadyToStart, Facetec, ViewPhrase
}
