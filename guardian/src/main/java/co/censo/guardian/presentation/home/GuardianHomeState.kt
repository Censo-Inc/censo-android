package co.censo.guardian.presentation.home

import Base64EncodedData
import InvitationId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.model.AcceptGuardianshipApiResponse
import co.censo.shared.data.model.ApproveRecoveryApiResponse
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.GuardianState
import co.censo.shared.data.model.RejectRecoveryApiResponse
import co.censo.shared.data.model.StoreRecoveryTotpSecretApiResponse
import co.censo.shared.data.model.SubmitGuardianVerificationApiResponse
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okhttp3.ResponseBody

data class GuardianHomeState(
    val verificationCode: String = "",
    val guardianState: GuardianState? = null,
    val invitationId: InvitationId = InvitationId(""),
    val participantId: String = "",
    val userResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val acceptGuardianResource: Resource<AcceptGuardianshipApiResponse> = Resource.Uninitialized,
    val declineGuardianResource: Resource<ResponseBody> = Resource.Uninitialized,
    val submitVerificationResource: Resource<SubmitGuardianVerificationApiResponse> = Resource.Uninitialized,
    val guardianUIState: GuardianUIState = GuardianUIState.UNINITIALIZED,
    val guardianEncryptionKey: EncryptionKey? = null,

    val storeRecoveryTotpSecretResource: Resource<StoreRecoveryTotpSecretApiResponse> = Resource.Uninitialized,
    val recoveryTotp: RecoveryTotpState? = null,
    val approveRecoveryResource: Resource<ApproveRecoveryApiResponse> = Resource.Uninitialized,
    val rejectRecoveryResource: Resource<RejectRecoveryApiResponse> = Resource.Uninitialized,
) {
    val apiError = userResponse is Resource.Error || acceptGuardianResource is Resource.Error
            || declineGuardianResource is Resource.Error || submitVerificationResource is Resource.Error
            || storeRecoveryTotpSecretResource is Resource.Error
            || approveRecoveryResource is Resource.Error
            || rejectRecoveryResource is Resource.Error

    data class RecoveryTotpState(
        val code: String,
        val counter: Long = Clock.System.now().epochSeconds.div(TotpGenerator.CODE_EXPIRATION),
        val currentSecond: Int = Clock.System.now().toLocalDateTime(TimeZone.UTC).second,
        val encryptedSecret: Base64EncodedData
    ) {
        val countdownPercentage = 1.0f - (currentSecond.toFloat() / TotpGenerator.CODE_EXPIRATION.toFloat())
    }
}

enum class GuardianUIState {
    UNINITIALIZED,
    INVITE_READY, //There is no guardian state in the user response.
    MISSING_INVITE_CODE,  //There is no guardian state in the user response, and app did not persist invite code.
    INVALID_PARTICIPANT_ID,
    DECLINED_INVITE, //Guardian declined invite. Temporary state until Guardian leaves app. Maybe just kick them out...
    NEED_SAVE_KEY, //Guardian state in user response is WAITING_FOR_CODE. Guardian has accepted invite. User has not saved private key.
    WAITING_FOR_CODE, //Guardian state in user response is WAITING_FOR_CODE. Guardian has accepted invite. User has saved private key.
    WAITING_FOR_CONFIRMATION, //Guardian state in user response is WAITING_FOR_CONFIRMATION.
    CODE_REJECTED, //Guardian state in user response is VerificationRejected
    COMPLETE, //Guardian state in user response is COMPLETE
    RECOVERY_REQUESTED,
    RECOVERY_WAITING_FOR_TOTP_FROM_OWNER,
    RECOVERY_VERIFYING_TOTP_FROM_OWNER,
}
