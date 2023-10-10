package co.censo.vault.presentation.recovery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.SharedScreen
import co.censo.shared.data.Resource
import co.censo.shared.data.model.ApprovalStatus
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.Recovery
import co.censo.shared.data.repository.OwnerRepository
import co.censo.vault.presentation.home.Screen
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
        state = state.copy(userResponse = Resource.Loading())

        viewModelScope.launch {
            val response = ownerRepository.retrieveUser()

            state = state.copy(userResponse = response)

            if (response is Resource.Success) {
                onOwnerState(response.data!!.ownerState)
            }
        }
    }

    private fun onOwnerState(ownerState: OwnerState) {
        when (ownerState) {
            is OwnerState.Ready -> {
                state = state.copy(
                    initiateNewRecovery = ownerState.policy.recovery == null,
                    recovery = ownerState.policy.recovery,
                    guardians = ownerState.policy.guardians,
                    secrets = ownerState.vault.secrets.map { it.guid },
                    approvalsCollected = ownerState.policy.recovery?.let {
                        when (it) {
                            is Recovery.ThisDevice -> it.approvals.count { it.status == ApprovalStatus.Approved }
                            else -> 0
                        }
                    } ?: 0,
                    approvalsRequired = ownerState.policy.threshold.toInt()
                )
            }

            else -> {
                // other owner states are not supported on this view
                // navigate back to start of the app so it can fix itself
                state = state.copy(
                    navigationResource = Resource.Success(SharedScreen.EntranceRoute.route)
                )
            }
        }
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

            state = state.copy(
                initiateRecoveryResource = response
            )

            if (response is Resource.Success) {
                onOwnerState(response.data!!.ownerState)
            }
        }
    }

    fun cancelRecovery() {
        state = state.copy(
            cancelRecoveryResource = Resource.Loading()
        )

        viewModelScope.launch {
            val response = ownerRepository.cancelRecovery()

            state = state.copy(
                cancelRecoveryResource = response
            )

            if (response is Resource.Success) {
                state = state.copy(
                    recovery = null,
                    navigationResource = Resource.Success(Screen.VaultScreen.route)
                )
            }
        }
    }
}
