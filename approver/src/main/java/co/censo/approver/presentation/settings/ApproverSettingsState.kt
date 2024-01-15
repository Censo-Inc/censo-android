package co.censo.approver.presentation.settings

import co.censo.shared.data.Resource
import co.censo.shared.data.model.GetApproverUserApiResponse
import co.censo.shared.util.NavigationData

data class ApproverSettingsState(
    val userResponse: Resource<GetApproverUserApiResponse> = Resource.Loading,
    val showDeleteUserConfirmDialog: Boolean = false,
    val deleteUserResource: Resource<Unit> = Resource.Uninitialized,
    val navigationResource: Resource<NavigationData> = Resource.Uninitialized,
) {

    val loading = userResponse is Resource.Loading
            || deleteUserResource is Resource.Loading
    val asyncError = userResponse is Resource.Error
            || deleteUserResource is Resource.Error
}
