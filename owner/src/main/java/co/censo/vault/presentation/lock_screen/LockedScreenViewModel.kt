package co.censo.vault.presentation.lock_screen

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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LockedScreenViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository
) : ViewModel() {

    var state by mutableStateOf(LockedScreenState())
        private set

    fun onStart() {
        retrieveOwnerState()
    }

    fun retrieveOwnerState() {
        state = state.copy(ownerStateResource = Resource.Loading())

        viewModelScope.launch {
            val ownerStateResource = ownerRepository.retrieveUser().map { it.ownerState }
            onOwnerState(ownerStateResource)
        }
    }

    private fun onOwnerState(ownerStateResource: Resource<OwnerState>) {
        state = state.copy(
            ownerStateResource = ownerStateResource,
        )

        if (ownerStateResource is Resource.Success) {
            val ownerState = ownerStateResource.data
            if (ownerState is OwnerState.Ready) {
                state = state.copy(
                    lockStatus = LockedScreenState.LockStatus.fromInstant(ownerState.locksAt)
                )
            }
        }
    }

    fun initUnlock() {
        state = state.copy(
            lockStatus = LockedScreenState.LockStatus.UnlockInProgress(
                apiCall = Resource.Uninitialized
            )
        )
    }

    suspend fun onFaceScanReady(verificationId: BiometryVerificationId, facetecData: FacetecBiometry): Resource<BiometryScanResultBlob> {
        state = state.copy(
            lockStatus = LockedScreenState.LockStatus.UnlockInProgress(
                apiCall = Resource.Loading()
            )
        )

        return viewModelScope.async {
            val unlockVaultResponse: Resource<UnlockApiResponse> = ownerRepository.unlock(verificationId, facetecData)

            state = state.copy(
                lockStatus = LockedScreenState.LockStatus.UnlockInProgress(
                    apiCall = unlockVaultResponse
                )
            )

            if (unlockVaultResponse is Resource.Success) {
                onOwnerState(unlockVaultResponse.map { it.ownerState })
            }

            unlockVaultResponse.map { it.scanResultBlob }
        }.await()
    }

    fun initLock() {
        state = state.copy(
            lockStatus = LockedScreenState.LockStatus.LockInProgress(
                apiCall = Resource.Loading()
            )
        )

        viewModelScope.launch {
            val lockVaultResponse: Resource<LockApiResponse> = ownerRepository.lock()

            state = state.copy(
                lockStatus = LockedScreenState.LockStatus.LockInProgress(
                    apiCall = lockVaultResponse
                )
            )

            if (lockVaultResponse is Resource.Success) {
                onOwnerState(lockVaultResponse.map { it.ownerState })
            }
        }
    }
}
