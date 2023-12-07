package co.censo.censo.presentation.plan_setup

import Base58EncodedGuardianPublicKey
import ParticipantId
import co.censo.censo.presentation.initial_plan_setup.InitialKeyData
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.model.CompleteOwnerGuardianshipApiResponse
import co.censo.shared.data.model.CreatePolicySetupApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.InitiateRecoveryApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.ReplacePolicyApiResponse
import co.censo.shared.data.model.RetrieveRecoveryShardsApiResponse
import co.censo.shared.presentation.cloud_storage.CloudStorageActionData
import kotlinx.datetime.Clock


data class PlanSetupState(
    val ownerState: OwnerState.Ready? = null,

    // restored approvers state
    val ownerApprover: Guardian.ProspectGuardian? = null,
    val primaryApprover: Guardian.ProspectGuardian? = null,
    val alternateApprover: Guardian.ProspectGuardian? = null,

    // Screen in Plan Setup Flow
    val planSetupDirection: PlanSetupDirection = PlanSetupDirection.AddApprovers,
    val planSetupUIState: PlanSetupUIState = PlanSetupUIState.Initial_1,

    // inviting approver
    val editedNickname: String = "",

    // totp
    val secondsLeft: Int = 0,
    val counter: Long = Clock.System.now().epochSeconds.div(TotpGenerator.CODE_EXPIRATION),
    val approverCodes: Map<ParticipantId, String> = emptyMap(),


    // API Calls
    val userResponse: Resource<OwnerState> = Resource.Uninitialized,
    val createPolicySetupResponse: Resource<CreatePolicySetupApiResponse> = Resource.Uninitialized,
    val initiateRecoveryResponse: Resource<InitiateRecoveryApiResponse> = Resource.Uninitialized,
    val retrieveRecoveryShardsResponse: Resource<RetrieveRecoveryShardsApiResponse> = Resource.Uninitialized,
    val replacePolicyResponse: Resource<ReplacePolicyApiResponse> = Resource.Uninitialized,
    val completeGuardianShipResponse : Resource<CompleteOwnerGuardianshipApiResponse> = Resource.Uninitialized,

    val verifyKeyConfirmationSignature: Resource<Unit> = Resource.Uninitialized,

    // Cloud Storage
    val cloudStorageAction: CloudStorageActionData = CloudStorageActionData(),

    val keyData: PlanSetupKeyData? = null,
    val saveKeyToCloud: Resource<Unit> = Resource.Uninitialized,

    // Navigation
    val navigationResource: Resource<String> = Resource.Uninitialized
) {
    companion object {
        const val APPROVER_NAME_MAX_LENGTH = 20
    }

    val activatingApprover = alternateApprover ?: primaryApprover
    val approverType = if (alternateApprover != null) ApproverType.Alternate else ApproverType.Primary

    val editedNicknameIsTooLong = editedNickname.length > APPROVER_NAME_MAX_LENGTH
    val editedNicknameValid = editedNickname.isNotEmpty() && !editedNicknameIsTooLong

    val backArrowType = when {
        planSetupUIState in listOf(
            PlanSetupUIState.ApproverActivation_5,
            PlanSetupUIState.EditApproverNickname_3
        ) -> BackIconType.Back

        planSetupUIState in listOf(
            PlanSetupUIState.ApproverGettingLive_4,
            PlanSetupUIState.EditApproverNickname_3
        ) && approverType == ApproverType.Alternate -> BackIconType.Back

        planSetupUIState in listOf(
            PlanSetupUIState.ApproverGettingLive_4,
            PlanSetupUIState.AddAlternateApprover_6,
            PlanSetupUIState.RecoveryInProgress_7
        ) -> BackIconType.Exit

        else -> BackIconType.None
    }

    val loading = userResponse is Resource.Loading
                || createPolicySetupResponse is Resource.Loading
                || initiateRecoveryResponse is Resource.Loading
                || retrieveRecoveryShardsResponse is Resource.Loading
                || replacePolicyResponse is Resource.Loading
                || saveKeyToCloud is Resource.Loading
                || completeGuardianShipResponse is Resource.Loading

    val asyncError = userResponse is Resource.Error
            || createPolicySetupResponse is Resource.Error
            || initiateRecoveryResponse is Resource.Error
            || retrieveRecoveryShardsResponse is Resource.Error
            || replacePolicyResponse is Resource.Error
            || verifyKeyConfirmationSignature is Resource.Error
            || completeGuardianShipResponse is Resource.Error
            || saveKeyToCloud is Resource.Error

    enum class BackIconType {
        None, Back, Exit
    }
}

enum class PlanSetupUIState {
    Initial_1,
    ApproverNickname_2,
    EditApproverNickname_3,
    ApproverGettingLive_4,
    ApproverActivation_5,
    AddAlternateApprover_6,
    RecoveryInProgress_7,
    Completed_8
}

sealed interface PlanSetupAction {

    //Approver Setup
    data class ApproverNicknameChanged(val name: String) : PlanSetupAction
    object EditApproverNickname : PlanSetupAction
    object EditApproverAndSavePolicy : PlanSetupAction
    object InviteApprover : PlanSetupAction
    object SaveApproverAndSavePolicy : PlanSetupAction
    object GoLiveWithApprover: PlanSetupAction
    object ApproverConfirmed : PlanSetupAction

    //Plan Finalization
    object Completed : PlanSetupAction
    object SavePlan: PlanSetupAction

    //Back
    object BackClicked : PlanSetupAction

    //Cloud
    object KeyUploadSuccess : PlanSetupAction
    data class KeyDownloadSuccess(val encryptedKey: ByteArray) : PlanSetupAction
    data class KeyDownloadFailed(val e: Exception?) : PlanSetupAction
    data class KeyUploadFailed(val e: Exception?) : PlanSetupAction

    //Retry
    object Retry : PlanSetupAction
}

enum class ApproverType {
    Primary, Alternate
}

data class PlanSetupKeyData(
    val encryptedPrivateKey: ByteArray,
    val publicKey: Base58EncodedGuardianPublicKey
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InitialKeyData

        if (!encryptedPrivateKey.contentEquals(other.encryptedPrivateKey)) return false
        if (publicKey != other.publicKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encryptedPrivateKey.contentHashCode()
        result = 31 * result + publicKey.hashCode()
        return result
    }
}