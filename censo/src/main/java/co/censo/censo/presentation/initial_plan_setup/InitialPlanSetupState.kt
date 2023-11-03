package co.censo.censo.presentation.initial_plan_setup

import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.generatePartitionId
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.model.CreatePolicyApiResponse
import co.censo.shared.data.repository.CreatePolicyParams
import co.censo.shared.presentation.cloud_storage.CloudStorageActionData

data class InitialPlanSetupScreenState(
    val initialPlanSetupStep: InitialPlanSetupStep = InitialPlanSetupStep.Initial,
    val participantId: ParticipantId = ParticipantId(generatePartitionId()),
    val saveKeyToCloudResource: Resource<Unit> = Resource.Uninitialized,
    val createPolicyParams: Resource<CreatePolicyParams> = Resource.Uninitialized,
    val createPolicyResponse: Resource<CreatePolicyApiResponse> = Resource.Uninitialized,
    val initialPlanData: InitialPlanData = InitialPlanData(),
    val complete: Boolean = false,
    val cloudStorageAction: CloudStorageActionData = CloudStorageActionData(),
) {
    val apiError = createPolicyParams is Resource.Error
        || createPolicyResponse is Resource.Error
        || saveKeyToCloudResource is Resource.Error
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

data class InitialPlanData(
    val createPolicyParams: CreatePolicyParams? = null,
    val approverEncryptionKey: EncryptionKey? = null,
)