package co.censo.approver.presentation.home

import Base64EncodedData
import co.censo.approver.data.ApproverAccessUIState
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.model.ApproveRecoveryApiResponse
import co.censo.shared.data.model.GetApproverUserApiResponse
import co.censo.shared.data.model.GuardianPhase
import co.censo.shared.data.model.GuardianState
import co.censo.shared.data.model.RejectRecoveryApiResponse
import co.censo.shared.data.model.StoreRecoveryTotpSecretApiResponse
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
    val userResponse: Resource<GetApproverUserApiResponse> = Resource.Uninitialized,
    val guardianEncryptionKey: EncryptionKey? = null,

    // recovery
    val recoveryTotp: RecoveryTotpState? = null,
    val storeRecoveryTotpSecretResource: Resource<StoreRecoveryTotpSecretApiResponse> = Resource.Uninitialized,
    val approveRecoveryResource: Resource<ApproveRecoveryApiResponse> = Resource.Uninitialized,
    val rejectRecoveryResource: Resource<RejectRecoveryApiResponse> = Resource.Uninitialized,
    val ownerEnteredWrongCode: Boolean = false,
    val accessNotInProgress: Resource<Unit> = Resource.Uninitialized,

    // UI state
    val approverAccessUIState: ApproverAccessUIState = ApproverAccessUIState.AccessRequested,
    val showTopBarCancelConfirmationDialog: Boolean = false,

    //Cloud Storage
    val cloudStorageAction: CloudStorageActionData = CloudStorageActionData(),
    val loadKeyFromCloudResource: Resource<Unit> = Resource.Uninitialized,
    val recoveryConfirmationPhase: GuardianPhase.RecoveryConfirmation? = null,

    val navToApproverRouting: Boolean = false,
) {

    val loading = userResponse is Resource.Loading
            || storeRecoveryTotpSecretResource is Resource.Loading
            || approveRecoveryResource is Resource.Loading
            || rejectRecoveryResource is Resource.Loading

    val asyncError = userResponse is Resource.Error
            || storeRecoveryTotpSecretResource is Resource.Error
            || approveRecoveryResource is Resource.Error
            || rejectRecoveryResource is Resource.Error
            || accessNotInProgress is Resource.Error

    val showTopBar = (!loading && !asyncError) && approverAccessUIState !is ApproverAccessUIState.Complete

    val shouldCheckRecoveryCode =
        userResponse !is Resource.Loading && guardianState?.phase is GuardianPhase.RecoveryVerification

    data class RecoveryTotpState(
        val code: String,
        val counter: Long = Clock.System.now().epochSeconds.div(TotpGenerator.CODE_EXPIRATION),
        val currentSecond: Int = Clock.System.now().toLocalDateTime(TimeZone.UTC).second,
        val encryptedSecret: Base64EncodedData
    )
}