package co.censo.approver.presentation.owners

import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.approver.presentation.Screen
import co.censo.shared.data.Resource
import co.censo.shared.data.repository.ApproverRepository
import co.censo.shared.util.asResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApproverOwnersListViewModel @Inject constructor(
    private val approverRepository: ApproverRepository
) : ViewModel() {

    var state by mutableStateOf(ApproverOwnersListState())
        private set

    fun onStart() {
        retrieveApproverState()
    }

    fun retrieveApproverState() {
        viewModelScope.launch {
            val userResponse = approverRepository.retrieveUser()
            state = state.copy(userResponse = userResponse)
        }
    }

    fun navToEditLabelScreen(participantId: ParticipantId) {
        state = state.copy(
            navigationResource = Screen.ApproverLabelOwnerScreen.navTo(participantId).asResource()
        )
    }

    fun resetNavigationResource() {
        state = state.copy(navigationResource = Resource.Uninitialized)
    }
}
