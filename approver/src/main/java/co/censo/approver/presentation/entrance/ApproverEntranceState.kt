package co.censo.approver.presentation.entrance

import co.censo.approver.data.ApproverEntranceUIState
import co.censo.shared.data.Resource
import okhttp3.ResponseBody

data class ApproverEntranceState(
    val uiState: ApproverEntranceUIState = ApproverEntranceUIState.Initial,
    val linkError: Boolean = false,

    val triggerGoogleSignIn: Resource<Unit> = Resource.Uninitialized,
    val signInUserResource: Resource<ResponseBody> = Resource.Uninitialized,
    val forceUserToGrantCloudStorageAccess: ForceUserToGrantCloudStorageAccess = ForceUserToGrantCloudStorageAccess(),

    val navigationResource: Resource<String> = Resource.Uninitialized,
) {
    val isLoading = signInUserResource is Resource.Loading

    val apiCallErrorOccurred = signInUserResource is Resource.Error
            || triggerGoogleSignIn is Resource.Error
}

data class ForceUserToGrantCloudStorageAccess(
    val requestAccess: Boolean = false,
    val jwt: String? = ""
)

enum class RoutingDestination {
    ONBOARDING, ACCESS
}
