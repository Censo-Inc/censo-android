package co.censo.guardian.presentation.home

import Base64EncodedData
import InvitationId
import co.censo.guardian.data.ApproverAccessUIState
import co.censo.guardian.presentation.onboarding.OnboardingMessage
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
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class ApproverAccessState(

    // guardian state
    val guardianState: GuardianState? = null,

    // deep links data
    val participantId: String = "",

    // Approver data
    val userResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val guardianEncryptionKey: EncryptionKey? = null,

    // recovery
    val recoveryTotp: RecoveryTotpState? = null,
    val storeRecoveryTotpSecretResource: Resource<StoreRecoveryTotpSecretApiResponse> = Resource.Uninitialized,
    val approveRecoveryResource: Resource<ApproveRecoveryApiResponse> = Resource.Uninitialized,
    val rejectRecoveryResource: Resource<RejectRecoveryApiResponse> = Resource.Uninitialized,
    val ownerEnteredWrongCode: Boolean = false,

    // UI state
    val approverAccessUIState: ApproverAccessUIState = ApproverAccessUIState.UserNeedsPasteRecoveryLink,
    val showTopBarCancelConfirmationDialog: Boolean = false,

    //Cloud Storage
    val cloudStorageAction: CloudStorageActionData = CloudStorageActionData(),
    val loadKeyFromCloudResource: Resource<Unit> = Resource.Uninitialized,
    val recoveryConfirmationPhase: GuardianPhase.RecoveryConfirmation? = null,

    //Success/Error Message
    val onboardingMessage: Resource<OnboardingMessage> = Resource.Uninitialized,
) {

    val loading = userResponse is Resource.Loading
            || storeRecoveryTotpSecretResource is Resource.Loading
            || approveRecoveryResource is Resource.Loading
            || rejectRecoveryResource is Resource.Loading

    val asyncError = userResponse is Resource.Error
            || storeRecoveryTotpSecretResource is Resource.Error
            || approveRecoveryResource is Resource.Error
            || rejectRecoveryResource is Resource.Error

    val getApproverState =
        userResponse !is Resource.Loading && guardianState?.phase is GuardianPhase.RecoveryVerification

    data class RecoveryTotpState(
        val code: String,
        val counter: Long = Clock.System.now().epochSeconds.div(TotpGenerator.CODE_EXPIRATION),
        val currentSecond: Int = Clock.System.now().toLocalDateTime(TimeZone.UTC).second,
        val encryptedSecret: Base64EncodedData
    )
}