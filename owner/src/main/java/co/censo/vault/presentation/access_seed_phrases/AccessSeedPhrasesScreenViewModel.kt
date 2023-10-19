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
import co.censo.shared.data.repository.OwnerRepository
import co.censo.vault.presentation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccessSeedPhrasesScreenViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
) : ViewModel() {

    var state by mutableStateOf(AccessSeedPhrasesScreenState())
        private set

    fun onStart() {

    }

    fun reset() {
        state = AccessSeedPhrasesScreenState()
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
                viewModelScope.launch {
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
                            val requestedSecrets = ownerState.vault.secrets.filter { recovery.vaultSecretIds.contains(it.guid) }

                            val recoveredSecrets: List<RecoveredSeedPhrase> = ownerRepository.recoverSecrets(
                                requestedSecrets,
                                encryptedShards,
                                ownerState.policy.encryptedMasterKey
                            )

                            state = state.copy(
                                recoveredPhrases = Resource.Success(recoveredSecrets)
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
                            navigationResource = Resource.Success(Screen.RecoveryScreen.route)
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
}
