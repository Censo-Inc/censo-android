package co.censo.censo.presentation.owner_key_recovery

import Base64EncodedData
import ParticipantId
import co.censo.censo.presentation.plan_finalization.ReplacePolicyKeyData
import co.censo.shared.data.Resource
import co.censo.shared.data.model.ReplacePolicyShardsApiResponse
import co.censo.shared.data.model.RetrieveAccessShardsApiResponse
import co.censo.shared.presentation.cloud_storage.CloudStorageActionData

data class OwnerKeyRecoveryState(
    val ownerParticipantId: ParticipantId? = null,
    val ownerEntropy: Base64EncodedData? = null,
    val encryptedMasterKey: Base64EncodedData? = null,
    val ownerKeyUIState: OwnerKeyRecoveryUIState = OwnerKeyRecoveryUIState.Initial,

    // API calls
    val retrieveAccessShardsResponse: Resource<RetrieveAccessShardsApiResponse> = Resource.Uninitialized,
    val verifyApproverKeysSignature: Resource<Unit> = Resource.Uninitialized,
    val replaceShardsResponse: Resource<ReplacePolicyShardsApiResponse> = Resource.Uninitialized,

    // Cloud Storage
    val cloudStorageAction: CloudStorageActionData = CloudStorageActionData(),
    val saveKeyToCloud: Resource<Unit> = Resource.Uninitialized,
    val keyData: ReplacePolicyKeyData? = null,


    val navigationResource: Resource<String> = Resource.Uninitialized
) {

    val loading = retrieveAccessShardsResponse is Resource.Loading
            || verifyApproverKeysSignature is Resource.Loading
            || saveKeyToCloud is Resource.Loading
            || replaceShardsResponse is Resource.Loading

    val asyncError = retrieveAccessShardsResponse is Resource.Error
            || verifyApproverKeysSignature is Resource.Error
            || saveKeyToCloud is Resource.Error
            || replaceShardsResponse is Resource.Error
}

enum class OwnerKeyRecoveryUIState {
    Initial, AccessInProgress, Completed
}

sealed interface KeyRecoveryAction {
    object Completed : KeyRecoveryAction
    object BackClicked : KeyRecoveryAction
    object KeyUploadSuccess : KeyRecoveryAction
    data class KeyUploadFailed(val e: Exception?) : KeyRecoveryAction
    object Retry : KeyRecoveryAction
}