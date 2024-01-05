package co.censo.approver.presentation.reset_links

import ParticipantId
import LoginIdResetToken
import co.censo.shared.data.Resource
import co.censo.shared.data.model.ApproverState
import co.censo.shared.data.model.GetApproverUserApiResponse

data class ResetLinksState(
    val approverStates: List<ApproverState> = listOf(),
    val uiState: ResetLinkUIState = ResetLinkUIState.ListApprovers(),

    // api calls
    val userResponse: Resource<GetApproverUserApiResponse> = Resource.Uninitialized,
    val createResetTokenResponse: Resource<GetApproverUserApiResponse> = Resource.Uninitialized,
) {
    val loading = userResponse is Resource.Loading
    val asyncError = userResponse is Resource.Error

    sealed class ResetLinkUIState {
        data class ListApprovers(val selectedParticipantId: ParticipantId? = null) : ResetLinkUIState()
        data class GettingLive(val participantId: ParticipantId) : ResetLinkUIState()
        data class ShareLink(val resetToken: LoginIdResetToken) : ResetLinkUIState()
    }
}