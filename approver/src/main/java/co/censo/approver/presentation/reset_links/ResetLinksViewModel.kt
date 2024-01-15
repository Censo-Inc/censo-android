package co.censo.approver.presentation.reset_links

import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.model.ApproverState
import co.censo.shared.data.repository.ApproverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResetLinksViewModel @Inject constructor(
    private val approverRepository: ApproverRepository
) : ViewModel() {

    var state by mutableStateOf(ResetLinksState())
        private set

    fun onStart() {
        retrieveApproverState()
    }

    fun retrieveApproverState() {
        state = state.copy(userResponse = Resource.Loading)

        viewModelScope.launch {
            val userResponse = approverRepository.retrieveUser()

            if (userResponse is Resource.Success) {
                onApproverStates(userResponse.data.approverStates)
            }

            state = state.copy(userResponse = userResponse)
        }
    }

    private fun onApproverStates(approverStates: List<ApproverState>) {
        if (approverStates.size == 1) {
            state = state.copy(
                approverStates = approverStates,
                uiState = ResetLinksState.ResetLinkUIState.GettingLive(approverStates.first().participantId)
            )
        } else {
            state = state.copy(
                approverStates = approverStates,
                uiState = ResetLinksState.ResetLinkUIState.ListApprovers()
            )
        }
    }

    fun onParticipantIdSelected(participantId: ParticipantId) {
        val alreadySelectedParticipantId = (state.uiState as? ResetLinksState.ResetLinkUIState.ListApprovers)?.selectedParticipantId

        state = if (alreadySelectedParticipantId == participantId) {
            state.copy(uiState = ResetLinksState.ResetLinkUIState.ListApprovers())
        } else {
            state.copy(uiState = ResetLinksState.ResetLinkUIState.ListApprovers(selectedParticipantId = participantId))
        }
    }

    fun continueToGetLiveWithOwner(participantId: ParticipantId) {
        state = state.copy(
            uiState = ResetLinksState.ResetLinkUIState.GettingLive(participantId)
        )
    }

    fun onGettingLive(participantId: ParticipantId) {
        state = state.copy(createResetTokenResponse = Resource.Loading)

        viewModelScope.launch {
            val response = approverRepository.createLoginIdResetToken(participantId.value)

            if (response is Resource.Success) {
                val resetToken = response.data.approverStates.first { it.participantId == participantId }.ownerLoginIdResetToken!!

                state = state.copy(
                    uiState = ResetLinksState.ResetLinkUIState.ShareLink(resetToken)
                )
            }

            state = state.copy(createResetTokenResponse = response)
        }
    }

}
