package co.censo.censo.presentation.access_seed_phrases

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.RecoveredSeedPhrase
import co.censo.shared.data.model.Access
import co.censo.shared.data.model.AccessStatus
import co.censo.shared.data.model.RetrieveAccessShardsApiResponse
import co.censo.shared.data.model.SeedPhrase
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.VaultCountDownTimer
import co.censo.censo.presentation.Screen
import co.censo.censo.presentation.Screen.PolicySetupRoute.navToAndPopCurrentDestination
import co.censo.shared.data.model.AccessIntent
import co.censo.shared.util.BIP39
import co.censo.shared.util.CrashReportingUtil.AccessPhrase
import co.censo.shared.util.NavigationData
import co.censo.shared.util.asResource
import co.censo.shared.util.sendError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class AccessSeedPhrasesViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val ownerStateFlow: MutableStateFlow<Resource<OwnerState>>,
    private val timer: VaultCountDownTimer,
) : ViewModel() {

    var state by mutableStateOf(AccessSeedPhrasesState())
        private set

    companion object {
        const val MULTI_APPROVER_POLICY = 2
    }

    fun onStart() {
        viewModelScope.launch {
            ownerStateFlow.collect { resource: Resource<OwnerState> ->
                if (resource is Resource.Success) {
                    state = state.copy(ownerState = resource)
                }
            }
        }

        timer.start(interval = 1.seconds.inWholeMilliseconds) {

            state.locksAt?.let {
                val now =  Clock.System.now()
                if (now >= it) {
                    reset()
                } else {
                    state = state.copy(timeRemaining = it - now)
                }
            }
        }
    }

    fun onStop() {
        timer.stop()

        if (state.viewedPhrase.isNotEmpty()) {
            reset()
        }
    }

    fun reset() {
        state = AccessSeedPhrasesState().copy(
            ownerState = state.ownerState,
            viewedPhraseIds = state.viewedPhraseIds
        )
    }

    fun retrieveOwnerState() {
        state = state.copy(ownerState = Resource.Loading)

        viewModelScope.launch {
            val ownerStateResource = ownerRepository.retrieveUser().map { it.ownerState }

            state = state.copy(ownerState = ownerStateResource)
        }
    }

    suspend fun onFaceScanReady(
        verificationId: BiometryVerificationId,
        biometry: FacetecBiometry
    ): Resource<BiometryScanResultBlob> {
        state = state.copy(retrieveShardsResponse = Resource.Loading)

        return viewModelScope.async {
            val response = ownerRepository.retrieveAccessShards(verificationId, biometry)

            state = state.copy(
                retrieveShardsResponse = response,
            )

            if (response is Resource.Success) {
                viewModelScope.launch(Dispatchers.IO) {
                    recoverSeedPhrases(response.data)
                }
                ownerStateFlow.tryEmit(response.map { it.ownerState })
            }

            response.map { it.scanResultBlob }
        }.await()
    }

    private suspend fun recoverSeedPhrases(response: RetrieveAccessShardsApiResponse) {
        val ownerState = response.ownerState
        val encryptedShards = response.encryptedShards

        when (ownerState) {
            is OwnerState.Ready -> {
                val access = ownerState.access

                when {
                    access is Access.ThisDevice && access.status == AccessStatus.Available -> {
                        state = state.copy(recoveredPhrases = Resource.Loading)

                        runCatching {
                            val requestedSeedPhrase = state.selectedPhrase

                            check(requestedSeedPhrase != null)

                            val recoveredSeedPhrases: List<RecoveredSeedPhrase> =
                                ownerRepository.recoverSeedPhrases(
                                    listOf(requestedSeedPhrase),
                                    encryptedShards,
                                    ownerState.policy.encryptedMasterKey,
                                    language = state.currentLanguage
                                )

                            state = state.copy(
                                recoveredPhrases = Resource.Success(recoveredSeedPhrases),
                                viewedPhrase = recoveredSeedPhrases.firstOrNull()?.phraseWords ?: listOf(""),
                                accessPhrasesUIState = AccessPhrasesUIState.ViewPhrase,
                                viewedPhraseIds = recoveredSeedPhrases.firstOrNull()?.let { state.viewedPhraseIds + it.guid } ?: state.viewedPhraseIds,
                                locksAt = Clock.System.now().plus(15.minutes)
                            )
                        }.onFailure {
                            Exception("Failed to recover seed phrases").sendError(AccessPhrase)
                            state = state.copy(
                                recoveredPhrases = Resource.Error(exception = Exception("Failed to recover seed phrases"))
                            )
                        }
                    }

                    else -> {
                        // there should be 'available' access requested by this device
                        // navigate back to access approval screen
                        state = state.copy(
                            navigationResource = Screen.AccessApproval
                                .withIntent(intent = AccessIntent.AccessPhrases)
                                .navToAndPopCurrentDestination()
                                .asResource()
                        )
                    }
                }
            }

            else -> {
                // other owner states are not supported on this view
                // navigate back to start of the app so it can fix itself
                state = state.copy(
                    navigationResource = Screen.EntranceRoute.navToAndPopCurrentDestination().asResource()
                )
            }
        }
    }

    fun resetNavigationResource() {
        state = state.copy(
            navigationResource = Resource.Uninitialized
        )
    }

    fun onPhraseSelected(seedPhrase: SeedPhrase) {
        state = state.copy(
            selectedPhrase = seedPhrase,
            accessPhrasesUIState = AccessPhrasesUIState.ReadyToStart
        )
    }

    fun startFacetec(language: BIP39.WordListLanguage?) {
        state = state.copy(
            accessPhrasesUIState = AccessPhrasesUIState.Facetec,
            currentLanguage = language
        )
    }

    fun onBackClicked() {
        val accessPhrasesUIState = when (state.accessPhrasesUIState) {
            AccessPhrasesUIState.ViewPhrase,
            AccessPhrasesUIState.ReadyToStart -> AccessPhrasesUIState.SelectPhrase
            AccessPhrasesUIState.SelectPhrase,
            AccessPhrasesUIState.Facetec -> null
        }

        accessPhrasesUIState?.let {
            state = state.copy(accessPhrasesUIState = it)
        }
    }

    fun cancelAccess() {
        state = state.copy(
            showCancelConfirmationDialog = false,
            cancelAccessResource = Resource.Loading
        )

        viewModelScope.launch {
            val response = ownerRepository.cancelAccess()

            if (response is Resource.Success) {
                state = state.copy(
                    navigationResource = Screen.OwnerVaultScreen
                        .navToAndPopCurrentDestination()
                        .asResource()
                )
                ownerStateFlow.tryEmit(response.map { it.ownerState })
            }

            state = state.copy(cancelAccessResource = response)
        }
    }

    fun hideCloseConfirmationDialog() {
        state = state.copy(showCancelConfirmationDialog = false)
    }

    fun showCancelConfirmationDialog() {
        val approverSize =
            (state.ownerState.success()?.data as? OwnerState.Ready)?.policy?.approvers?.size ?: MULTI_APPROVER_POLICY

        val timelockInEffect =
            (state.ownerState.success()?.data as? OwnerState.Ready)?.timelockSetting?.currentTimelockInSeconds != null

        if (approverSize > 1 || timelockInEffect) {
            state = state.copy(showCancelConfirmationDialog = true)
        } else {
            cancelAccess()
        }
    }
}
