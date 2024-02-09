package co.censo.censo.presentation.initial_plan_setup

import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.generatePartitionId
import co.censo.shared.data.model.CreatePolicyApiResponse
import co.censo.shared.data.model.InitialKeyData
import co.censo.shared.data.repository.CreatePolicyParams

data class InitialPlanSetupScreenState(
    val welcomeStep: WelcomeStep = WelcomeStep.Authenticated,
    val initialPlanSetupStep: InitialPlanSetupStep = InitialPlanSetupStep.Initial,
    val participantId: ParticipantId = ParticipantId(generatePartitionId()),
    val saveKeyToCloudResource: Resource<Unit> = Resource.Uninitialized,
    val loadKeyFromCloudResource: Resource<Unit> = Resource.Uninitialized,
    val createPolicyParamsResponse: Resource<CreatePolicyParams> = Resource.Uninitialized,
    val createPolicyResponse: Resource<CreatePolicyApiResponse> = Resource.Uninitialized,
    val createPolicyParams: CreatePolicyParams? = null,
    val complete: Boolean = false,
    val keyData: InitialKeyData? = null,
    val triggerDeleteUserDialog: Resource<Unit> = Resource.Uninitialized,
    val deleteUserResource: Resource<Unit> = Resource.Uninitialized,
    val kickUserOut: Resource<Unit> = Resource.Uninitialized,

    val showPromoCodeUI: Boolean = false,
    val promoCodeLoading: Boolean = false,
    val promoCode: String = "",
    val promoCodeAccepted: Boolean = false,
    val promoCodeSuccessDialog: Boolean = false,
    val promoCodeErrorDialog: Boolean = false,
) {
    val apiError = createPolicyParamsResponse is Resource.Error
        || createPolicyResponse is Resource.Error
        || saveKeyToCloudResource is Resource.Error
        || loadKeyFromCloudResource is Resource.Error
        || deleteUserResource is Resource.Error
}

sealed class InitialPlanSetupStep {

    //Entering the screen we will move to following 3 states
    data object Initial : InitialPlanSetupStep()
    data object CreateApproverKey : InitialPlanSetupStep()
    data object CreatePolicyParams : InitialPlanSetupStep()
    //After policy params are made then we will trigger facetec
    data object Facetec : InitialPlanSetupStep()
    //Policy creation always kicked off by facetec completion
    data object PolicyCreation : InitialPlanSetupStep()
    //User can drop out from the flow leading to deletion
    data object DeleteUser : InitialPlanSetupStep()
}

enum class WelcomeStep(val order: Int) {
    Authenticated(1), ScanningFace(2)
}