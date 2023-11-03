package co.censo.approver.presentation.routing

import co.censo.shared.data.Resource
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.GuardianState

data class ApproverRoutingState(

    // guardian state
    val guardianState: GuardianState? = null,

    // async resources
    val userResponse: Resource<GetUserApiResponse> = Resource.Loading(),
    val navToApproverAccess: Resource<Unit> = Resource.Uninitialized,
    val navToApproverOnboarding: Resource<Unit> = Resource.Uninitialized
)

enum class RoutingDestination {
    ONBOARDING, ACCESS
}
