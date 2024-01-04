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
import co.censo.shared.data.repository.KeyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OwnerKeyValidationViewModel @Inject constructor(
    private val keyRepository: KeyRepository,
    private val keyValidationTrigger: MutableSharedFlow<String>,
    ) : ViewModel() {

    var state by mutableStateOf(OwnerKeyValidationState())
        private set

    fun onCreate() {
        viewModelScope.launch {
            keyValidationTrigger.collect { participantIdString ->
                validateApproverKey(ParticipantId(participantIdString))
            }
        }
    }

    private fun validateApproverKey(participantId: ParticipantId) {
        viewModelScope.launch(Dispatchers.IO) {
            val hasKeySavedInCloud = runCatching {
                val downloadResult = keyRepository.retrieveKeyFromCloud(participantId, bypassScopeCheck = true)
                downloadResult is Resource.Success && downloadResult.data?.isNotEmpty() == true
            }.getOrElse { true } // Defaults to true in case of any trouble

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
