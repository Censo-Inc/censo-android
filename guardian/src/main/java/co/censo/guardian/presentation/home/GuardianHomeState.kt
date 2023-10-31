package co.censo.guardian.presentation.home

import Base64EncodedData
import InvitationId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.model.AcceptGuardianshipApiResponse
import co.censo.shared.data.model.ApproveRecoveryApiResponse
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.GuardianPhase
import co.censo.shared.data.model.GuardianState
import co.censo.shared.data.model.RejectRecoveryApiResponse
import co.censo.shared.data.model.StoreRecoveryTotpSecretApiResponse
import co.censo.shared.data.model.SubmitGuardianVerificationApiResponse
import co.censo.shared.presentation.cloud_storage.CloudStorageActionData
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class GuardianHomeState(

    // guardian state
    val guardianState: GuardianState? = null,

    // deep links data
    val invitationId: InvitationId = InvitationId(""),
    val participantId: String = "",

    // onboarding
    val verificationCode: String = "",
    val guardianEncryptionKey: EncryptionKey? = null,
    val userResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val acceptGuardianResource: Resource<AcceptGuardianshipApiResponse> = Resource.Uninitialized,
    val submitVerificationResource: Resource<SubmitGuardianVerificationApiResponse> = Resource.Uninitialized,

    // recovery
    val recoveryTotp: RecoveryTotpState? = null,
    val storeRecoveryTotpSecretResource: Resource<StoreRecoveryTotpSecretApiResponse> = Resource.Uninitialized,
    val approveRecoveryResource: Resource<ApproveRecoveryApiResponse> = Resource.Uninitialized,
    val rejectRecoveryResource: Resource<RejectRecoveryApiResponse> = Resource.Uninitialized,

    // UI state
    val guardianUIState: GuardianUIState = GuardianUIState.MISSING_INVITE_CODE,
    val showTopBarCancelConfirmationDialog: Boolean = false,

    //Cloud Storage
    val savePrivateKeyToCloudResource: Resource<Unit> = Resource.Uninitialized,
    val cloudStorageAction: CloudStorageActionData = CloudStorageActionData(),
    val recoveryConfirmationPhase: GuardianPhase.RecoveryConfirmation? = null
) {

    val loading = userResponse is Resource.Loading
            || acceptGuardianResource is Resource.Loading
            || submitVerificationResource is Resource.Loading
            || storeRecoveryTotpSecretResource is Resource.Loading
            || approveRecoveryResource is Resource.Loading
            || rejectRecoveryResource is Resource.Loading

    val asyncError = userResponse is Resource.Error
            || acceptGuardianResource is Resource.Error
            || submitVerificationResource is Resource.Error
            || storeRecoveryTotpSecretResource is Resource.Error
            || approveRecoveryResource is Resource.Error
            || rejectRecoveryResource is Resource.Error
            || savePrivateKeyToCloudResource is Resource.Error

    data class RecoveryTotpState(
        val code: String,
        val counter: Long = Clock.System.now().epochSeconds.div(TotpGenerator.CODE_EXPIRATION),
        val currentSecond: Int = Clock.System.now().toLocalDateTime(TimeZone.UTC).second,
        val encryptedSecret: Base64EncodedData
    )
}

enum class GuardianUIState {
    // Default
    MISSING_INVITE_CODE,        // There is no guardian state in the user response, and app did not persist invite code.

    // Onboarding
    INVITE_READY,               //There is no guardian state in the user response.
    WAITING_FOR_CODE,           //Guardian state in user response is WAITING_FOR_CODE. Guardian has accepted invite. User has saved private key.
    WAITING_FOR_CONFIRMATION,   //Guardian state in user response is WAITING_FOR_CONFIRMATION.
    CODE_REJECTED,              //Guardian state in user response is VerificationRejected
    COMPLETE,                   //Guardian state in user response is COMPLETE

    // Access
    INVALID_PARTICIPANT_ID,
    ACCESS_REQUESTED,
    ACCESS_WAITING_FOR_TOTP_FROM_OWNER,
    ACCESS_VERIFYING_TOTP_FROM_OWNER,
    ACCESS_APPROVED,
}

enum class GuardianHomeCloudStorageReasons {
    NONE, CONFIRM_OR_REJECT_OWNER, SUBMIT_VERIFICATION_CODE
}
