package co.censo.vault.presentation.vault

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.model.DeleteSecretApiResponse
import co.censo.shared.data.model.LockStatus
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.StoreSecretApiResponse
import co.censo.shared.data.model.VaultSecret
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.data.storage.Storage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val storage: Storage,
    private val ownerRepository: OwnerRepository
) : ViewModel() {

    var state by mutableStateOf(VaultState())
        private set

    fun onNewOwnerState(ownerState: OwnerState.Ready) {
        state = state.copy(ownerState = ownerState)
    }

    fun isLocked(ownerState: OwnerState.Ready): Boolean {
        return when (LockStatus.fromOwnerState(ownerState)) {
            is LockStatus.Unlocked -> false
            else -> true
        }
    }

    fun addSecret(label: String, bipPhase: String, updateOwnerState: (OwnerState) -> Unit) {
        state = state.copy(
            storeSeedPhraseResource = Resource.Loading()
        )

        viewModelScope.async {
            val response: Resource<StoreSecretApiResponse> = ownerRepository.storeSecret(state.ownerState!!.vault.publicMasterEncryptionKey, label, bipPhase)

            if (response is Resource.Success) {
                response.data?.ownerState?.also {
                    updateOwnerState(it)
                }
            }

            state = state.copy(
                storeSeedPhraseResource = response
            )
        }
    }

    fun deleteSecret(secret: VaultSecret, updateOwnerState: (OwnerState) -> Unit) {
        state = state.copy(
            deleteSeedPhraseResource = Resource.Loading()
        )

        viewModelScope.async {
            val response: Resource<DeleteSecretApiResponse> = ownerRepository.deleteSecret(secret.guid)

            if (response is Resource.Success) {
                response.data?.ownerState?.also {
                    updateOwnerState(it)
                }
            }

            state = state.copy(
                deleteSeedPhraseResource = response
            )
        }
    }

}
