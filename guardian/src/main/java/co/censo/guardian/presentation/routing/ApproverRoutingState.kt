package co.censo.guardian.presentation.routing

import co.censo.guardian.data.ApproverAccessUIState
import co.censo.guardian.data.ApproverOnboardingUIState
import co.censo.guardian.presentation.home.GuardianUIState
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.GuardianState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class ApproverRoutingState(

    // guardian state
    val guardianState: GuardianState? = null,

    // UI State
    val guardianUIState: GuardianUIState = GuardianUIState.MISSING_INVITE_CODE,

    // async resources
    val userResponse: Resource<GetUserApiResponse> = Resource.Loading(),
    val navToGuardianHome: Resource<Unit> = Resource.Uninitialized,
    val navToApproverOnboarding: Resource<Unit> = Resource.Uninitialized
)

enum class RoutingDestination {
    ONBOARDING, ACCESS
}
