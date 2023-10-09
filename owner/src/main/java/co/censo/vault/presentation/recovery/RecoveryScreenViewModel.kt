package co.censo.vault.presentation.recovery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.repository.OwnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecoveryScreenViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository
) : ViewModel() {

    var state by mutableStateOf(RecoveryScreenState())
        private set

    fun onStart() {
        reloadOwnerState()
    }

    fun reloadOwnerState() {
        state = state.copy(ownerStateResource = Resource.Loading())

        viewModelScope.launch {
            val response = ownerRepository.retrieveUser().map { it.ownerState }.asReady()

            state = state.copy(ownerStateResource = response)

            if (response is Resource.Success) {
                onOwnerState(response.data!!)
            }
        }
    }

    private fun onOwnerState(ownerState: OwnerState.Ready) {
        state = state.copy(
            initiateNewRecovery = ownerState.policy.recovery == null,
            recovery = ownerState.policy.recovery,
            guardians = ownerState.policy.guardians,
            secrets = ownerState.vault.secrets.map { it.guid }
        )
    }

    fun reset() {
        state = RecoveryScreenState()
    }

    fun initiateRecovery() {
        state = state.copy(
            initiateNewRecovery = false,
            initiateRecoveryResource = Resource.Loading()
        )
        viewModelScope.launch {
            val response = ownerRepository.initiateRecovery(state.secrets)
            val ownerStateResource = response.map { it.ownerState }.asReady()

            state = state.copy(
                ownerStateResource = ownerStateResource,
                initiateRecoveryResource = response
            )

            if (ownerStateResource is Resource.Success) {
                onOwnerState(ownerStateResource.data!!)
            }
        }
    }

}

private fun Resource<OwnerState>.asReady(): Resource<OwnerState.Ready> {
    return this.flatMap {
        when (it) {
            is OwnerState.Ready -> Resource.Success(it)
            else -> Resource.Error(exception = Exception("Unexpected owner state"))
        }
    }
}

