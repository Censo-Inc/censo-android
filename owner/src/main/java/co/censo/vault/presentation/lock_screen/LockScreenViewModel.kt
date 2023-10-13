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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LockScreenViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val ownerStateFlow: MutableStateFlow<Resource<OwnerState>>
) : ViewModel() {

    var state by mutableStateOf(LockScreenState())
        private set

    fun onStart() {
        retrieveOwnerState()

        viewModelScope.launch {
            ownerStateFlow.collect { resource: Resource<OwnerState> ->
                if (resource is Resource.Success) {
                    onOwnerState(resource.data!!)
                }
            }
        }
    }

    fun retrieveOwnerState() {
        state = state.copy(ownerStateResource = Resource.Loading())

        viewModelScope.launch {
            val ownerStateResource = ownerRepository.retrieveUser().map { it.ownerState }

            state = state.copy(ownerStateResource = ownerStateResource)

            if (ownerStateResource is Resource.Success) {
                onOwnerState(ownerStateResource.data!!)
            }
        }
    }

    fun resetToLocked() {
        state = state.copy(lockStatus = LockScreenState.LockStatus.Locked)
    }

    private fun onOwnerState(ownerState: OwnerState) {
        state = state.copy(
            lockStatus = LockScreenState.LockStatus.fromOwnerState(ownerState)
        )
    }

    fun initUnlock() {
        state = state.copy(
            lockStatus = LockScreenState.LockStatus.UnlockInProgress(
                apiCall = Resource.Uninitialized
            )
        )
    }

    suspend fun onFaceScanReady(
        verificationId: BiometryVerificationId,
        facetecData: FacetecBiometry
    ): Resource<BiometryScanResultBlob> {
        state = state.copy(
            lockStatus = LockScreenState.LockStatus.UnlockInProgress(
                apiCall = Resource.Loading()
            )
        )

        return viewModelScope.async {
            val unlockVaultResponse: Resource<UnlockApiResponse> =
                ownerRepository.unlock(verificationId, facetecData)

            state = state.copy(
                lockStatus = LockScreenState.LockStatus.UnlockInProgress(
                    apiCall = unlockVaultResponse
                ),
                ownerStateResource = unlockVaultResponse.map { it.ownerState },
            )

            if (unlockVaultResponse is Resource.Success) {
                onOwnerState(unlockVaultResponse.data!!.ownerState)
            }

            unlockVaultResponse.map { it.scanResultBlob }
        }.await()
    }
}
