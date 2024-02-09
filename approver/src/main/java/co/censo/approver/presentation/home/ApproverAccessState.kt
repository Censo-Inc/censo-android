package co.censo.approver.presentation.home

import Base64EncodedData
import co.censo.approver.data.ApproverAccessUIState
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.model.ApproveAccessApiResponse
import co.censo.shared.data.model.GetApproverUserApiResponse
import co.censo.shared.data.model.ApproverPhase
import co.censo.shared.data.model.ApproverState
import co.censo.shared.data.model.RejectAccessApiResponse
import co.censo.shared.data.model.StoreAccessTotpSecretApiResponse
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class ApproverAccessState(

    // approver state
    val approverState: ApproverState? = null,

    // deep links data
    val approvalId: String = "",
    val participantId: String = "",

    // Approver data
    val userResponse: Resource<GetApproverUserApiResponse> = Resource.Uninitialized,
    val approverEncryptionKey: EncryptedKey? = null,

    // access
    val accessTotp: AccessTotpState? = null,
    val storeAccessTotpSecretResource: Resource<StoreAccessTotpSecretApiResponse> = Resource.Uninitialized,
    val approveAccessResource: Resource<ApproveAccessApiResponse> = Resource.Uninitialized,
    val rejectAccessResource: Resource<RejectAccessApiResponse> = Resource.Uninitialized,
    val ownerEnteredWrongCode: Boolean = false,
    val accessNotInProgress: Resource<Unit> = Resource.Uninitialized,

    // UI state
    val approverAccessUIState: ApproverAccessUIState = ApproverAccessUIState.AccessRequested,
    val showTopBarCancelConfirmationDialog: Boolean = false,

    //Cloud Storage
    val loadKeyFromCloudResource: Resource<Unit> = Resource.Uninitialized,
    val accessConfirmationPhase: ApproverPhase.AccessConfirmation? = null,

    val navToApproverEntrance: Boolean = false,
) {

    val loading = userResponse is Resource.Loading
            || storeAccessTotpSecretResource is Resource.Loading
            || approveAccessResource is Resource.Loading
            || rejectAccessResource is Resource.Loading

    val asyncError = userResponse is Resource.Error
            || storeAccessTotpSecretResource is Resource.Error
            || approveAccessResource is Resource.Error
            || rejectAccessResource is Resource.Error
            || accessNotInProgress is Resource.Error

    val showTopBar = (!loading && !asyncError) && approverAccessUIState !is ApproverAccessUIState.Complete

    val shouldCheckAccessCode =
        userResponse !is Resource.Loading && approverState?.phase is ApproverPhase.AccessVerification

    data class AccessTotpState(
        val code: String,
        val counter: Long = Clock.System.now().epochSeconds.div(TotpGenerator.CODE_EXPIRATION),
        val currentSecond: Int = Clock.System.now().toLocalDateTime(TimeZone.UTC).second,
        val encryptedSecret: Base64EncodedData
    )
}

data class EncryptedKey(val key: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedKey

        if (!key.contentEquals(other.key)) return false

        return true
    }

    override fun hashCode(): Int {
        return key.contentHashCode()
    }
}