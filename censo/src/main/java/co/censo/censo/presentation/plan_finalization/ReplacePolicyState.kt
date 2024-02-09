package co.censo.censo.presentation.plan_finalization

import Base58EncodedApproverPublicKey
import co.censo.censo.presentation.plan_setup.PolicySetupAction
import co.censo.shared.data.Resource
import co.censo.shared.data.model.CompleteOwnerApprovershipApiResponse
import co.censo.shared.data.model.CreatePolicySetupApiResponse
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.InitialKeyData
import co.censo.shared.data.model.InitiateAccessApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.ReplacePolicyApiResponse
import co.censo.shared.data.model.RetrieveAccessShardsApiResponse


data class ReplacePolicyState(
    val ownerState: OwnerState.Ready? = null,

    val replacePolicyUIState: ReplacePolicyUIState = ReplacePolicyUIState.Uninitialized_1,
    val policySetupAction: PolicySetupAction = PolicySetupAction.AddApprovers,

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
    val keyData: ReplacePolicyKeyData? = null,
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

    val backArrowType = when {
        replacePolicyUIState in listOf(
            ReplacePolicyUIState.AccessInProgress_2,
        ) -> BackIconType.EXIT

        else -> BackIconType.NONE
    }

    enum class BackIconType {
        NONE, EXIT
    }
}

enum class ReplacePolicyUIState {
    Uninitialized_1,
    AccessInProgress_2,
    Completed_3
}

sealed interface ReplacePolicyAction {

    //Plan Finalization
    object Completed : ReplacePolicyAction

    //Cloud
    object KeyUploadSuccess : ReplacePolicyAction
    data class KeyDownloadSuccess(val encryptedKey: ByteArray) : ReplacePolicyAction
    data class KeyDownloadFailed(val e: Exception?) : ReplacePolicyAction
    data class KeyUploadFailed(val e: Exception?) : ReplacePolicyAction

    //Retry
    object Retry : ReplacePolicyAction
    object BackClicked : ReplacePolicyAction
    object FacetecCancelled : ReplacePolicyAction
}

data class ReplacePolicyKeyData(
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