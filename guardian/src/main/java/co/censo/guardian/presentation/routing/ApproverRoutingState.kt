package co.censo.guardian.presentation.routing

import InvitationId
import co.censo.guardian.presentation.home.GuardianUIState
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.GuardianState

data class ApproverRoutingState(

    // guardian state
    val guardianState: GuardianState? = null,

    // UI State
    val guardianUIState: GuardianUIState = GuardianUIState.MISSING_INVITE_CODE,

    // async resources
    val userResponse: Resource<GetUserApiResponse> = Resource.Loading(),
    val navToGuardianHome: Resource<GuardianUIState> = Resource.Uninitialized
)
