package co.censo.approver.presentation.owners

import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GetApproverUserApiResponse

data class LabelOwnerState(
    val participantId: ParticipantId = ParticipantId.generate(),
    val labelResource: Resource<String> = Resource.Loading,
    val labelIsTooLong: Boolean = false,
    val saveEnabled: Boolean = false,
    val saveResource: Resource<GetApproverUserApiResponse> = Resource.Uninitialized
) {
    val loading = labelResource is Resource.Loading
    val asyncError = saveResource is Resource.Error
            || labelResource is Resource.Error
}