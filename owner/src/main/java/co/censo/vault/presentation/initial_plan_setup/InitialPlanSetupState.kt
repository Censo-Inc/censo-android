package co.censo.vault.presentation.initial_plan_setup

import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.generatePartitionId
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.model.CreatePolicyApiResponse
import co.censo.shared.data.repository.CreatePolicyParams

data class InitialPlanSetupScreenState(
    val initialPlanSetupStatus: InitialPlanSetupStatus = InitialPlanSetupStatus.None,
    val participantId: ParticipantId = ParticipantId(generatePartitionId()),
    val approverEncryptionKey: EncryptionKey? = null,
    val saveKeyToCloudResource: Resource<Unit> = Resource.Uninitialized,
    val createPolicyParams: Resource<CreatePolicyParams> = Resource.Uninitialized,
    val complete: Boolean = false
) {

    sealed class InitialPlanSetupStatus {
        object None : InitialPlanSetupStatus()

        object CreatingPolicyParams : InitialPlanSetupStatus()

        object Initial : InitialPlanSetupStatus()

        object ApproverKeyCreationFailed : InitialPlanSetupStatus()

        object CreatePolicyParamsFailed : InitialPlanSetupStatus()

        data class CreateInProgress(val apiCall: Resource<CreatePolicyApiResponse>) :
            InitialPlanSetupStatus()

    }
}