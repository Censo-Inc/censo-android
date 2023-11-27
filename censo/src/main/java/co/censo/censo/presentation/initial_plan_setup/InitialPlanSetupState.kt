package co.censo.censo.presentation.initial_plan_setup

import Base58EncodedPublicKey
import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.generatePartitionId
import co.censo.shared.data.model.CreatePolicyApiResponse
import co.censo.shared.data.repository.CreatePolicyParams
import co.censo.shared.presentation.cloud_storage.CloudStorageActionData

data class InitialPlanSetupScreenState(
    val initialPlanSetupStep: InitialPlanSetupStep = InitialPlanSetupStep.Initial,
    val participantId: ParticipantId = ParticipantId(generatePartitionId()),
    val saveKeyToCloudResource: Resource<Unit> = Resource.Uninitialized,
    val loadKeyFromCloudResource: Resource<Unit> = Resource.Uninitialized,
    val createPolicyParamsResponse: Resource<CreatePolicyParams> = Resource.Uninitialized,
    val createPolicyResponse: Resource<CreatePolicyApiResponse> = Resource.Uninitialized,
    val createPolicyParams: CreatePolicyParams? = null,
    val complete: Boolean = false,
    val keyData: InitialKeyData? = null,
    val cloudStorageAction: CloudStorageActionData = CloudStorageActionData(),
) {
    val apiError = createPolicyParamsResponse is Resource.Error
        || createPolicyResponse is Resource.Error
        || saveKeyToCloudResource is Resource.Error
        || loadKeyFromCloudResource is Resource.Error
}

sealed class InitialPlanSetupStep {

    //Entering the screen we will move to following 3 states
    object Initial : InitialPlanSetupStep()
    object CreateApproverKey : InitialPlanSetupStep()
    object CreatePolicyParams : InitialPlanSetupStep()
    //After policy params are made then we will trigger facetec
    object Facetec : InitialPlanSetupStep()
    //Policy creation always kicked off by facetec completion
    object PolicyCreation : InitialPlanSetupStep()
}

data class InitialKeyData(
    val encryptedPrivateKey: ByteArray,
    val publicKey: Base58EncodedPublicKey
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