package co.censo.censo.presentation.routing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.repository.OwnerRepository
import co.censo.censo.presentation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OwnerRoutingViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val ownerStateFlow: MutableStateFlow<Resource<OwnerState>>,
) : ViewModel() {
    var state by mutableStateOf(OwnerRoutingState())
        private set

    fun onStart() {
        retrieveOwnerState(false)
    }

    fun onStop() {
        state = OwnerRoutingState()
    }

    fun retrieveOwnerState(silently: Boolean) {
        if (!silently) {
            state = state.copy(userResponse = Resource.Loading())
        }

        viewModelScope.launch {
            val userResponse = ownerRepository.retrieveUser()

            state = state.copy(userResponse = userResponse)

            if (userResponse is Resource.Success) {
                // update global state
                ownerStateFlow.tryEmit(userResponse.map { it.ownerState })

                determineUIState(userResponse.data!!.ownerState)
            }
        }
    }

    private fun determineUIState(ownerState: OwnerState) {
        if (ownerState is OwnerState.Ready && ownerState.vault.secrets.isNotEmpty()) {
            val destination = when {
                ownerState.guardianSetup != null -> Screen.PlanSetupRoute.buildNavRoute(welcomeFlow = false)
                ownerState.recovery != null -> Screen.AccessApproval.route
                else -> Screen.OwnerVaultScreen.route
            }
            state = state.copy(navigationResource = Resource.Success(destination))
        } else {
            state = state.copy(navigationResource = Resource.Success(Screen.OwnerWelcomeScreen.route))
        }
    }

    fun resetNavigationResource() {
        state = state.copy(navigationResource = Resource.Uninitialized)
    }
}