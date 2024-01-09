package co.censo.approver.presentation.owners

import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.repository.ApproverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LabelOwnerViewModel @Inject constructor(
    private val approverRepository: ApproverRepository
) : ViewModel() {

    var state by mutableStateOf(LabelOwnerState())
        private set

    fun onStart(participantId: ParticipantId) {
        viewModelScope.launch {
            state = state.copy(
                participantId = participantId,
                labelResource = approverRepository
                    .retrieveUser()
                    .map {
                        it.approverStates.firstOrNull { approverState ->
                            approverState.participantId == participantId
                        }?.ownerLabel ?: ""
                    }
            )
        }
    }

    fun onLabelChanged(newValue: String) {
        val labelIsTooLong = newValue.length > OWNER_LABEL_MAX_LENGTH
        state = state.copy(
            labelResource = Resource.Success(newValue),
            labelIsTooLong = labelIsTooLong,
            saveEnabled = newValue.isNotBlank() && !labelIsTooLong
        )
    }

    fun save() {
        state = state.copy(saveResource = Resource.Loading())
        viewModelScope.launch {
            if (state.labelResource is Resource.Success) {
                val response = approverRepository.labelOwner(state.participantId.value, state.labelResource.data!!)
                state = state.copy(saveResource = response)
            }
        }
    }

    fun resetSaveResource() {
        state = state.copy(saveResource = Resource.Uninitialized)
    }
}
