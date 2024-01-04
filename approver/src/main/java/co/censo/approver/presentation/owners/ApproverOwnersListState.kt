package co.censo.approver.presentation.owners

import co.censo.shared.data.Resource
import co.censo.shared.data.model.GetApproverUserApiResponse
import co.censo.shared.util.NavigationData

data class ApproverOwnersListState(
    val userResponse: Resource<GetApproverUserApiResponse> = Resource.Loading(),
    val navigationResource: Resource<NavigationData> = Resource.Uninitialized,
) {
    val loading = userResponse is Resource.Loading
    val asyncError = userResponse is Resource.Error
}
