package co.censo.censo.presentation.plan_setup

import Base58EncodedGuardianPublicKey
import ParticipantId
import co.censo.censo.presentation.initial_plan_setup.InitialKeyData
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.model.CompleteOwnerGuardianshipApiResponse
import co.censo.shared.data.model.ConfirmGuardianshipApiResponse
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
    val planSetupUIState: PlanSetupUIState = PlanSetupUIState.Initial,

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
            PlanSetupUIState.ApproverActivation,
            PlanSetupUIState.EditApproverNickname
        ) -> BackIconType.Back

        planSetupUIState in listOf(
            PlanSetupUIState.ApproverGettingLive,
            PlanSetupUIState.EditApproverNickname
        ) && approverType == ApproverType.Alternate -> BackIconType.Back

        planSetupUIState in listOf(
            PlanSetupUIState.ApproverGettingLive,
            PlanSetupUIState.AddAlternateApprover,
            PlanSetupUIState.RecoveryInProgress
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

    enum class BackIconType {
        None, Back, Exit
    }
}

enum class PlanSetupUIState {
    Initial,
    ApproverNickname,
    EditApproverNickname,
    ApproverGettingLive,
    ApproverActivation,
    AddAlternateApprover,
    RecoveryInProgress,
    Completed
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