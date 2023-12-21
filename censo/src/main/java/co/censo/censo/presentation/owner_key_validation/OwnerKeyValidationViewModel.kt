package co.censo.censo.presentation.owner_key_validation

import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.censo.presentation.Screen
import co.censo.shared.data.Resource
import co.censo.shared.data.model.AccessIntent
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.repository.KeyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OwnerKeyValidationViewModel @Inject constructor(
    private val keyRepository: KeyRepository,
    private val ownerStateFlow: MutableStateFlow<Resource<OwnerState>>
) : ViewModel() {

    var state by mutableStateOf(OwnerKeyValidationState())
        private set

    fun onCreate() {
        viewModelScope.launch {
            ownerStateFlow.collectLatest { resource: Resource<OwnerState> ->
                (resource.data as? OwnerState.Ready)?.let { ownerState ->
                    val externalApprovers = ownerState.policy.approvers.any { !it.isOwner }
                    val participantId = ownerState.policy.approvers.firstOrNull { it.isOwner }?.participantId

                    if (
                        externalApprovers && participantId != null      // external approvers are present
                        && state.participantId == null                  // was not checked before
                    ) {
                        state = state.copy(participantId = participantId)
                        validateApproverKey(participantId)
                    }
                }
            }
        }
    }

    private fun validateApproverKey(participantId: ParticipantId) {
        viewModelScope.launch(Dispatchers.IO) {
            val hasKeySavedInCloud = runCatching {
                keyRepository.userHasKeySavedInCloud(participantId)
            }.getOrNull() ?: true // skip check in case of missing Google Drive permissions

            state = if (hasKeySavedInCloud) {
                state.copy(ownerKeyUIState = OwnerKeyValidationState.OwnerKeyValidationUIState.None)
            } else {
                state.copy(ownerKeyUIState = OwnerKeyValidationState.OwnerKeyValidationUIState.FileNotFound)
            }
        }
    }

    fun navigateToKeyRecovery() {
        state = state.copy(
            navigationResource = Resource.Success(
                Screen.AccessApproval.withIntent(AccessIntent.RecoverOwnerKey)
            )
        )
    }

    fun resetAfterNavigation() {
        state = state.copy(
            navigationResource = Resource.Uninitialized,
            ownerKeyUIState = OwnerKeyValidationState.OwnerKeyValidationUIState.None
        )
    }
}
