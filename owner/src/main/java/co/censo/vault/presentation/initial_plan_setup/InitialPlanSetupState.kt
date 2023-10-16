package co.censo.vault.presentation.initial_plan_setup

import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.generatePartitionId
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.cryptography.key.ExternalEncryptionKey
import co.censo.shared.data.cryptography.toHexString
import co.censo.shared.data.model.CreatePolicyApiResponse
import co.censo.shared.data.model.CreatePolicySetupApiResponse
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.LockApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.SecurityPlanData
import co.censo.shared.data.model.UnlockApiResponse
import co.censo.vault.presentation.components.security_plan.SetupSecurityPlanScreen
import co.censo.vault.presentation.activate_approvers.ActivateApproversViewModel.Companion.MIN_GUARDIAN_LIMIT
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class InitialPlanSetupScreenState(
    val initialPlanSetupStatus: InitialPlanSetupStatus = InitialPlanSetupStatus.None,
    val participantId: ParticipantId = ParticipantId(generatePartitionId()),
    val approverEncryptionKey: EncryptionKey? = null,
    val saveKeyToCloudResource: Resource<Unit> = Resource.Uninitialized,
    val complete: Boolean = false
) {

    sealed class InitialPlanSetupStatus {
        object None : InitialPlanSetupStatus()

        object Initial : InitialPlanSetupStatus()

        object ApproverKeyCreationFailed : InitialPlanSetupStatus()

        data class SetupInProgress(val apiCall: Resource<CreatePolicySetupApiResponse>) :
            InitialPlanSetupStatus()

        data class CreateInProgress(val apiCall: Resource<CreatePolicyApiResponse>) :
            InitialPlanSetupStatus()

    }
}