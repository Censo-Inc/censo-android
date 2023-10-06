package co.censo.vault.presentation.vault

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.model.VaultSecret
import co.censo.shared.data.repository.OwnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultHomeViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository
) : ViewModel() {

    var state by mutableStateOf(VaultHomeState())
        private set

    fun onStart() {
        retrieveOwnerState()
    }

    fun retrieveOwnerState() {
        state = state.copy(ownerStateResource = Resource.Loading())
        viewModelScope.launch {
            val ownerStateResource = ownerRepository.retrieveUser().map { it.ownerState }
            state = state.copy(
                ownerStateResource = ownerStateResource,
            )
        }
    }

    fun deleteSecret(secret: VaultSecret) {
        state = state.copy(
            deleteSeedPhraseResource = Resource.Loading()
        )

        viewModelScope.async {
            val response = ownerRepository.deleteSecret(secret.guid)

            state = state.copy(
                deleteSeedPhraseResource = response
            )

            if (response is Resource.Success) {
                state = state.copy(
                    ownerStateResource = response.map { it.ownerState },
                )
            }
        }
    }

    fun resetStoreSeedPhraseResponse() {
        state.copy(storeSeedPhraseResource = Resource.Uninitialized )
    }

    fun resetDeleteSeedPhraseResponse() {
        state.copy(deleteSeedPhraseResource = Resource.Uninitialized )
    }

}
