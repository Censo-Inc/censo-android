package co.censo.vault.presentation.vault

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.SharedScreen
import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.VaultSecret
import co.censo.shared.data.repository.OwnerRepository
import co.censo.vault.presentation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultScreenViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val ownerStateFlow: MutableStateFlow<Resource<OwnerState>>
) : ViewModel() {

    var state by mutableStateOf(VaultScreenState())
        private set

    fun onStart() {
        retrieveOwnerState()
    }

    fun retrieveOwnerState() {
        state = state.copy(userResponse = Resource.Loading())

        viewModelScope.launch {
            val response = ownerRepository.retrieveUser()

            state = state.copy(
                userResponse = response
            )

            if (response is Resource.Success) {
                onOwnerState(response.data!!.ownerState)
            }
        }
    }

    fun deleteSecret(secret: VaultSecret) {
        state = state.copy(
            deleteSeedPhraseResource = Resource.Loading()
        )

        viewModelScope.launch {
            val response = ownerRepository.deleteSecret(secret.guid)

            state = state.copy(
                deleteSeedPhraseResource = response
            )

            if (response is Resource.Success) {
                onOwnerState(response.data!!.ownerState)
            }
        }
    }

    fun deleteUser() {
        state = state.copy(
            deleteUserResource = Resource.Loading()
        )

        val participantId = state.ownerState?.policy?.guardians?.get(0)?.participantId

        viewModelScope.launch(Dispatchers.IO) {
            val response = ownerRepository.deleteUser(participantId)

            state = state.copy(
                deleteUserResource = response
            )

            if (response is Resource.Success) {
                onOwnerState(OwnerState.Initial)
                ownerStateFlow.tryEmit(Resource.Success(OwnerState.Initial))
            }
        }
    }

    private fun onOwnerState(ownerState: OwnerState) {
        state = when (ownerState) {
            is OwnerState.Ready -> {
                state.copy(ownerState = ownerState)
            }

            else -> {
                // other owner states are not supported on this view
                // navigate back to start of the app so it can fix itself
                state.copy(navigationResource = Resource.Success(SharedScreen.EntranceRoute.route))
            }
        }
    }

    fun resetDeleteUserResource() {
        state = state.copy(deleteUserResource = Resource.Uninitialized)
    }

    fun resetStoreSeedPhraseResponse() {
        state = state.copy(storeSeedPhraseResource = Resource.Uninitialized)
    }

    fun resetDeleteSeedPhraseResponse() {
        state = state.copy(deleteSeedPhraseResource = Resource.Uninitialized)
    }

    fun onEditSeedPhrases() {
        state = state.copy(screen = VaultScreens.EditSeedPhrases)
    }

    fun onRecoverPhrases() {
        state = state.copy(navigationResource = Resource.Success(Screen.RecoveryScreen.route))
    }

    fun onResetUser() {
        state = state.copy(screen = VaultScreens.ResetUser)
    }

    fun onCancelResetUser() {
        state = state.copy(screen = VaultScreens.Unlocked)
    }

    fun reset() {
        state = VaultScreenState()
    }

}
