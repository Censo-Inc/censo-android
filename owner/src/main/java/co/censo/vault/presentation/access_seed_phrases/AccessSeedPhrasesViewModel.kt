package co.censo.vault.presentation.access_seed_phrases

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.SharedScreen
import co.censo.shared.data.Resource
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.RecoveredSeedPhrase
import co.censo.shared.data.model.Recovery
import co.censo.shared.data.model.RecoveryStatus
import co.censo.shared.data.model.RetrieveRecoveryShardsApiResponse
import co.censo.shared.data.model.VaultSecret
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.VaultCountDownTimer
import co.censo.vault.presentation.Screen
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

    fun onStart() {
        viewModelScope.launch {
            ownerStateFlow.collect { resource: Resource<OwnerState> ->
                if (resource is Resource.Success) {
                    state = state.copy(ownerState = resource)
                }
            }
        }

        timer.startCountDownTimer(countdownInterval = 1.seconds.inWholeMilliseconds) {

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
        timer.stopCountDownTimer()

        if (state.viewedPhrase.isNotEmpty()) {
            reset()
        }
    }

    fun reset() {
        state = AccessSeedPhrasesState().copy(
            ownerState = state.ownerState
        )
    }

    fun retrieveOwnerState() {
        state = state.copy(ownerState = Resource.Loading())

        viewModelScope.launch {
            val ownerStateResource = ownerRepository.retrieveUser().map { it.ownerState }

            state = state.copy(ownerState = ownerStateResource)
        }
    }

    suspend fun onFaceScanReady(
        verificationId: BiometryVerificationId,
        biometry: FacetecBiometry
    ): Resource<BiometryScanResultBlob> {
        state = state.copy(retrieveShardsResponse = Resource.Loading())

        return viewModelScope.async {
            val response = ownerRepository.retrieveRecoveryShards(verificationId, biometry)

            state = state.copy(
                retrieveShardsResponse = response,
            )

            if (response is Resource.Success) {
                viewModelScope.launch(Dispatchers.IO) {
                    recoverSecrets(response.data!!)
                }
            }

            response.map { it.scanResultBlob }
        }.await()
    }

    private suspend fun recoverSecrets(response: RetrieveRecoveryShardsApiResponse) {
        val ownerState = response.ownerState
        val encryptedShards = response.encryptedShards

        when (ownerState) {
            is OwnerState.Ready -> {
                val recovery = ownerState.recovery

                when {
                    recovery is Recovery.ThisDevice && recovery.status == RecoveryStatus.Available -> {
                        state = state.copy(recoveredPhrases = Resource.Loading())

                        runCatching {
                            val requestedSecret = state.selectedPhrase

                            check(requestedSecret != null)

                            val recoveredSecrets: List<RecoveredSeedPhrase> =
                                ownerRepository.recoverSecrets(
                                    listOf(requestedSecret),
                                    encryptedShards,
                                    ownerState.policy.encryptedMasterKey
                                )

                            state = state.copy(
                                recoveredPhrases = Resource.Success(recoveredSecrets),
                                viewedPhrase = recoveredSecrets.firstOrNull()?.seedPhrase?.split(" ") ?: listOf(""),
                                accessPhrasesUIState = AccessPhrasesUIState.ViewPhrase,
                                locksAt = Clock.System.now().plus(15.minutes)
                            )
                        }.onFailure {
                            state = state.copy(
                                recoveredPhrases = Resource.Error(exception = Exception(it))
                            )
                        }
                    }

                    else -> {
                        // there should be 'available' recovery requested by this device
                        // navigate back to recovery screen
                        state = state.copy(
                            navigationResource = Resource.Success(Screen.AccessApproval.route)
                        )
                    }
                }
            }

            else -> {
                // other owner states are not supported on this view
                // navigate back to start of the app so it can fix itself
                state = state.copy(
                    navigationResource = Resource.Success(SharedScreen.EntranceRoute.route)
                )
            }
        }
    }

    fun resetNavigationResource() {
        state = state.copy(
            navigationResource = Resource.Uninitialized
        )
    }

    fun onPhraseSelected(vaultSecret: VaultSecret) {
        state = state.copy(
            selectedPhrase = vaultSecret,
            accessPhrasesUIState = AccessPhrasesUIState.ReadyToStart
        )
    }

    fun startFacetec() {
        state = state.copy(
            accessPhrasesUIState = AccessPhrasesUIState.Facetec
        )
    }

    fun decrementIndex() {
        val index = if (state.selectedIndex > 0) state.selectedIndex - 1 else 0
        state = state.copy(selectedIndex = index)
    }

    fun incrementIndex() {
        val index =
            if (state.selectedIndex >= state.viewedPhrase.size - 1) state.selectedIndex else state.selectedIndex + 1
        state = state.copy(selectedIndex = index)
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
        state = state.copy(cancelRecoveryResource = Resource.Loading())

        viewModelScope.launch {
            val response = ownerRepository.cancelRecovery()

            if (response is Resource.Success) {
                state = state.copy(
                    navigationResource = Resource.Success(SharedScreen.OwnerVaultScreen.route)
                )
            }

            state = state.copy(cancelRecoveryResource = response)
        }
    }
}
