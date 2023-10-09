package co.censo.vault.presentation.vault

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.VaultSecret
import co.censo.shared.data.repository.OwnerRepository
import co.censo.vault.presentation.home.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultScreenViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository
) : ViewModel() {

    var state by mutableStateOf(VaultScreenState())
        private set

    fun onStart() {
        retrieveOwnerState()
    }

    fun retrieveOwnerState() {
        state = state.copy(ownerStateResource = Resource.Loading())
        viewModelScope.launch {
            val response = ownerRepository.retrieveUser()

            onOwnerStateReady(response.map { it.ownerState })
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

            onOwnerStateReady(response.map { it.ownerState })
        }
    }

    private fun onOwnerStateReady(ownerStateResource: Resource<OwnerState>) {
        val ready = ownerStateResource.flatMap {
            when (it) {
                is OwnerState.Ready -> Resource.Success(it)
                else -> Resource.Error(exception = Exception("Unexpected owner state"))
            }
        }

        state = state.copy(
            ownerStateResource = ready,
        )
    }

    fun resetStoreSeedPhraseResponse() {
        state.copy(storeSeedPhraseResource = Resource.Uninitialized )
    }

    fun resetDeleteSeedPhraseResponse() {
        state.copy(deleteSeedPhraseResource = Resource.Uninitialized )
    }

    fun onEditSeedPhrases() {
        state = state.copy(screen = VaultScreens.EditSeedPhrases)
    }

    fun onRecoverPhrases() {
        state = state.copy(navigationResource = Resource.Success(Screen.RecoveryScreen.route))
    }

    fun reset() {
        state = VaultScreenState()
    }

}
