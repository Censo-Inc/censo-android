package co.censo.approver.presentation.routing

import co.censo.shared.data.Resource
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.GuardianState

data class ApproverRoutingState(

    val showPasteLink: Boolean = false,

    val linkError: Boolean = false,

    val hasApprovers: Boolean = false,

    // async resources
    val navToApproverAccess: Resource<Unit> = Resource.Uninitialized,
    val navToApproverOnboarding: Resource<Unit> = Resource.Uninitialized
)

enum class RoutingDestination {
    ONBOARDING, ACCESS
}
