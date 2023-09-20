package co.censo.guardian.presentation.home

import InvitationId
import co.censo.shared.data.Resource
import co.censo.shared.data.model.AcceptGuardianshipApiResponse
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.GuardianState
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.SubmitGuardianVerificationApiResponse
import okhttp3.ResponseBody

data class GuardianHomeState(
    val ownerState: OwnerState? = null,
    val guardianStates: List<GuardianState> = emptyList(),
    val invitationId: InvitationId = InvitationId(""),
    val userResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val acceptGuardianResource: Resource<AcceptGuardianshipApiResponse> = Resource.Uninitialized,
    val declineGuardianResource: Resource<ResponseBody> = Resource.Uninitialized,
    val submitVerificationResource: Resource<SubmitGuardianVerificationApiResponse> = Resource.Uninitialized,
    val guardianUIState: GuardianUIState = GuardianUIState.UNINITIALIZED,
) {
    val apiError = userResponse is Resource.Error || acceptGuardianResource is Resource.Error
            || declineGuardianResource is Resource.Error || submitVerificationResource is Resource.Error
}

enum class GuardianUIState {
    UNINITIALIZED, USER_LOADED, HAS_INVITE_CODE, ACCEPTED_INVITE, DECLINED_INVITE, VERIFIED
}
