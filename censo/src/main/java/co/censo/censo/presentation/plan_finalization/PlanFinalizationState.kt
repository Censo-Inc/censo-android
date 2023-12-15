package co.censo.censo.presentation.plan_finalization

import Base58EncodedApproverPublicKey
import ParticipantId
import co.censo.censo.presentation.initial_plan_setup.InitialKeyData
import co.censo.censo.presentation.plan_setup.ApproverType
import co.censo.censo.presentation.plan_setup.PlanSetupDirection
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.model.CompleteOwnerApprovershipApiResponse
import co.censo.shared.data.model.CreatePolicySetupApiResponse
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.InitiateAccessApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.ReplacePolicyApiResponse
import co.censo.shared.data.model.RetrieveAccessShardsApiResponse
import co.censo.shared.presentation.cloud_storage.CloudStorageActionData
import kotlinx.datetime.Clock


data class PlanFinalizationState(
    val ownerState: OwnerState.Ready? = null,

    val planFinalizationUIState: PlanFinalizationUIState = PlanFinalizationUIState.Uninitialized_0,
    val planSetupDirection: PlanSetupDirection = PlanSetupDirection.AddApprovers,

    // restored approvers state
    val ownerApprover: Approver.ProspectApprover? = null,
    val primaryApprover: Approver.ProspectApprover? = null,
    val alternateApprover: Approver.ProspectApprover? = null,


    // API Calls
    val userResponse: Resource<OwnerState> = Resource.Uninitialized,
    val createPolicySetupResponse: Resource<CreatePolicySetupApiResponse> = Resource.Uninitialized,
    val initiateAccessResponse: Resource<InitiateAccessApiResponse> = Resource.Uninitialized,
    val retrieveAccessShardsResponse: Resource<RetrieveAccessShardsApiResponse> = Resource.Uninitialized,
    val replacePolicyResponse: Resource<ReplacePolicyApiResponse> = Resource.Uninitialized,
    val completeApprovershipResponse : Resource<CompleteOwnerApprovershipApiResponse> = Resource.Uninitialized,

    val verifyKeyConfirmationSignature: Resource<Unit> = Resource.Uninitialized,

    // Cloud Storage
    val cloudStorageAction: CloudStorageActionData = CloudStorageActionData(),

    val keyData: PlanFinalizationKeyData? = null,
    val saveKeyToCloud: Resource<Unit> = Resource.Uninitialized,

    // Navigation
    val navigationResource: Resource<String> = Resource.Uninitialized
) {

    val loading = userResponse is Resource.Loading
            || createPolicySetupResponse is Resource.Loading
            || initiateAccessResponse is Resource.Loading
            || retrieveAccessShardsResponse is Resource.Loading
            || replacePolicyResponse is Resource.Loading
            || saveKeyToCloud is Resource.Loading
            || completeApprovershipResponse is Resource.Loading

    val asyncError = userResponse is Resource.Error
            || createPolicySetupResponse is Resource.Error
            || initiateAccessResponse is Resource.Error
            || retrieveAccessShardsResponse is Resource.Error
            || replacePolicyResponse is Resource.Error
            || verifyKeyConfirmationSignature is Resource.Error
            || completeApprovershipResponse is Resource.Error
            || saveKeyToCloud is Resource.Error
}

enum class PlanFinalizationUIState {
    Uninitialized_0,
    AccessInProgress_1,
    Completed_2
}

sealed interface PlanFinalizationAction {

    //Plan Finalization
    object Completed : PlanFinalizationAction

    //Cloud
    object KeyUploadSuccess : PlanFinalizationAction
    data class KeyDownloadSuccess(val encryptedKey: ByteArray) : PlanFinalizationAction
    data class KeyDownloadFailed(val e: Exception?) : PlanFinalizationAction
    data class KeyUploadFailed(val e: Exception?) : PlanFinalizationAction

    //Retry
    object Retry : PlanFinalizationAction
    object FacetecCancelled : PlanFinalizationAction
}

data class PlanFinalizationKeyData(
    val encryptedPrivateKey: ByteArray,
    val publicKey: Base58EncodedApproverPublicKey
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