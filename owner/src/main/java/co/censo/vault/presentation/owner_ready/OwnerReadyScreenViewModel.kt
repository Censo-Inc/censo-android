package co.censo.vault.presentation.owner_ready

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.data.model.LockApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.UnlockApiResponse
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.data.storage.Storage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OwnerReadyScreenViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository
) : ViewModel() {

    var state by mutableStateOf(OwnerReadyScreenState())
        private set

    fun onNewOwnerState(ownerState: OwnerState.Ready) {
        state = state.updateOwnerState(ownerState)
    }

    fun initUnlock() {
        state = state.copy(
            lockStatus = OwnerReadyScreenState.LockStatus.UnlockInProgress(
                apiCall = Resource.Uninitialized
            )
        )
    }

    suspend fun onFaceScanReady(verificationId: BiometryVerificationId, facetecData: FacetecBiometry, updateOwnerState: (OwnerState) -> Unit): Resource<BiometryScanResultBlob> {
        state = state.copy(
            lockStatus = OwnerReadyScreenState.LockStatus.UnlockInProgress(
                apiCall = Resource.Loading()
            )
        )

        return viewModelScope.async {
            val unlockVaultResponse: Resource<UnlockApiResponse> = ownerRepository.unlock(verificationId, facetecData)

            state = state.copy(
                lockStatus = OwnerReadyScreenState.LockStatus.UnlockInProgress(
                    apiCall = unlockVaultResponse
                )
            )

            if (unlockVaultResponse is Resource.Success) {
                unlockVaultResponse.data?.ownerState?.also {
                    updateOwnerState(it)
                }
            }

            unlockVaultResponse.map { it.scanResultBlob }
        }.await()
    }

    fun initLock(updateOwnerState: (OwnerState) -> Unit) {
        state = state.copy(
            lockStatus = OwnerReadyScreenState.LockStatus.LockInProgress(
                apiCall = Resource.Loading()
            )
        )

        viewModelScope.launch {
            val lockVaultResponse: Resource<LockApiResponse> = ownerRepository.lock()

            state = state.copy(
                lockStatus = OwnerReadyScreenState.LockStatus.LockInProgress(
                    apiCall = lockVaultResponse
                )
            )

            if (lockVaultResponse is Resource.Success) {
                lockVaultResponse.data?.ownerState?.also {
                    updateOwnerState(it)
                }
            }
        }
    }
}
