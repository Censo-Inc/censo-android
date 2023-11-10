package co.censo.censo.presentation.plan_setup

import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.cryptography.key.EncryptionKey
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

    val verifyKeyConfirmationSignature: Resource<Unit> = Resource.Uninitialized,

    // Cloud Storage
    val cloudStorageAction: CloudStorageActionData = CloudStorageActionData(),
    val tempOwnerApprover: Guardian.SetupGuardian.ImplicitlyOwner? = null,
    //TODO: Perform SymmetricEncryption on the key in the next PR
    val tempEncryptedKey: EncryptionKey? = null,
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
            PlanSetupUIState.ApproverNickname,
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

    val asyncError = userResponse is Resource.Error
            || createPolicySetupResponse is Resource.Error
            || initiateRecoveryResponse is Resource.Error
            || retrieveRecoveryShardsResponse is Resource.Error
            || replacePolicyResponse is Resource.Error
            || verifyKeyConfirmationSignature is Resource.Error

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