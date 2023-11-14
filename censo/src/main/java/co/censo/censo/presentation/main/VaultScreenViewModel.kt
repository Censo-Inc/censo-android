package co.censo.censo.presentation.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.VaultSecret
import co.censo.shared.data.repository.OwnerRepository
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

    fun deleteSecret() {
        if (state.triggerEditPhraseDialog !is Resource.Success && state.triggerEditPhraseDialog.data == null) {
            state = state.copy(
                deleteSeedPhraseResource = Resource.Error()
            )
            return
        }

        val vaultSecret = state.triggerEditPhraseDialog.data

        state = state.copy(
            triggerEditPhraseDialog = Resource.Uninitialized,
            deleteSeedPhraseResource = Resource.Loading()
        )

        viewModelScope.launch {
            val response = ownerRepository.deleteSecret(vaultSecret!!.guid)

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

        val participantId =
            state.ownerState?.policy?.guardians?.first { it.isOwner }?.participantId

        viewModelScope.launch(Dispatchers.IO) {
            val response = ownerRepository.deleteUser(participantId)

            state = state.copy(
                deleteUserResource = response
            )

            if (response is Resource.Success) {
                onOwnerState(OwnerState.Initial)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            ownerRepository.signUserOut()
            state = state.copy(kickUserOut = Resource.Success(Unit))
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
                state.copy(kickUserOut = Resource.Success(Unit))
            }
        }

        ownerStateFlow.tryEmit(Resource.Success(ownerState))
    }

    fun resetDeleteUserResource() {
        state = state.copy(deleteUserResource = Resource.Uninitialized)
    }

    fun resetDeleteSeedPhraseResponse() {
        state = state.copy(deleteSeedPhraseResource = Resource.Uninitialized)
    }

    fun showDeleteUserDialog() {
        state = state.copy(triggerDeleteUserDialog = Resource.Success(Unit))
    }

    fun showEditPhraseDialog(vaultSecret: VaultSecret) {
        state = state.copy(triggerEditPhraseDialog = Resource.Success(vaultSecret))
    }

    fun onCancelDeletePhrase() {
        state = state.copy(triggerEditPhraseDialog = Resource.Uninitialized)
    }

    fun onCancelResetUser() {
        state = state.copy(triggerDeleteUserDialog = Resource.Uninitialized)
    }

    fun reset() {
        state = VaultScreenState()
    }

    fun lock() {
        state = state.copy(lockResponse = Resource.Loading())
        viewModelScope.launch {
            val lockResponse = ownerRepository.lock()

            if (lockResponse is Resource.Success) {
                onOwnerState(OwnerState.Initial)
            }
        }
    }

}
