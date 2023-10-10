package co.censo.guardian.presentation.home

import InvitationId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.model.AcceptGuardianshipApiResponse
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.GuardianState
import co.censo.shared.data.model.SubmitGuardianVerificationApiResponse
import okhttp3.ResponseBody

data class GuardianHomeState(
    val verificationCode: String = "",
    val guardianState: GuardianState? = null,
    val invitationId: InvitationId = InvitationId(""),
    val userResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val acceptGuardianResource: Resource<AcceptGuardianshipApiResponse> = Resource.Uninitialized,
    val declineGuardianResource: Resource<ResponseBody> = Resource.Uninitialized,
    val submitVerificationResource: Resource<SubmitGuardianVerificationApiResponse> = Resource.Uninitialized,
    val guardianUIState: GuardianUIState = GuardianUIState.UNINITIALIZED,
    val guardianEncryptionKey: EncryptionKey? = null
) {
    val apiError = userResponse is Resource.Error || acceptGuardianResource is Resource.Error
            || declineGuardianResource is Resource.Error || submitVerificationResource is Resource.Error
}

enum class GuardianUIState {
    UNINITIALIZED,
    INVITE_READY, //There is no guardian state in the user response.
    MISSING_INVITE_CODE,  //There is no guardian state in the user response, and app did not persist invite code.
    DECLINED_INVITE, //Guardian declined invite. Temporary state until Guardian leaves app. Maybe just kick them out...
    NEED_SAVE_KEY, //Guardian state in user response is WAITING_FOR_CODE. Guardian has accepted invite. User has not saved private key.
    WAITING_FOR_CODE, //Guardian state in user response is WAITING_FOR_CODE. Guardian has accepted invite. User has saved private key.
    WAITING_FOR_CONFIRMATION, //Guardian state in user response is WAITING_FOR_CONFIRMATION.
    CODE_REJECTED, //Guardian state in user response is VerificationRejected
    COMPLETE //Guardian state in user response is COMPLETE
}
