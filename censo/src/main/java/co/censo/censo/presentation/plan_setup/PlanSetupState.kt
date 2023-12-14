package co.censo.censo.presentation.plan_setup

import Base58EncodedApproverPublicKey
import ParticipantId
import co.censo.censo.presentation.initial_plan_setup.InitialKeyData
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


data class PlanSetupState(
    val ownerState: OwnerState.Ready? = null,

    // restored approvers state
    val ownerApprover: Approver.ProspectApprover? = null,
    val primaryApprover: Approver.ProspectApprover? = null,
    val alternateApprover: Approver.ProspectApprover? = null,

    // Screen in Plan Setup Flow
    val planSetupDirection: PlanSetupDirection = PlanSetupDirection.AddApprovers,
    val planSetupUIState: PlanSetupUIState = PlanSetupUIState.Initial_1,

    // inviting approver
    val editedNickname: String = "",

    // totp
    val secondsLeft: Int = 0,
    val counter: Long = Clock.System.now().epochSeconds.div(TotpGenerator.CODE_EXPIRATION),
    val approverCodes: Map<ParticipantId, String> = emptyMap(),


    // API Calls
    val userResponse: Resource<OwnerState> = Resource.Uninitialized,
    val createPolicySetupResponse: Resource<CreatePolicySetupApiResponse> = Resource.Uninitialized,
    val initiateAccessResponse: Resource<InitiateAccessApiResponse> = Resource.Uninitialized,
    val retrieveAccessShardsResponse: Resource<RetrieveAccessShardsApiResponse> = Resource.Uninitialized,
    val completeApprovershipResponse : Resource<CompleteOwnerApprovershipApiResponse> = Resource.Uninitialized,

    val verifyKeyConfirmationSignature: Resource<Unit> = Resource.Uninitialized,

    // Navigation
    val navigationResource: Resource<String> = Resource.Uninitialized,

    // Plan Finalization
    val finalizePlanSetup: Resource<Unit> = Resource.Uninitialized,
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
            PlanSetupUIState.ApproverActivation_5,
            PlanSetupUIState.EditApproverNickname_3
        ) -> BackIconType.Back

        planSetupUIState in listOf(
            PlanSetupUIState.ApproverGettingLive_4,
            PlanSetupUIState.AddAlternateApprover_6,
            PlanSetupUIState.AccessInProgress_7
        ) -> BackIconType.Exit

        else -> BackIconType.None
    }

    val loading = userResponse is Resource.Loading
                || createPolicySetupResponse is Resource.Loading
                || initiateAccessResponse is Resource.Loading
                || retrieveAccessShardsResponse is Resource.Loading
                || completeApprovershipResponse is Resource.Loading

    val asyncError = userResponse is Resource.Error
            || createPolicySetupResponse is Resource.Error
            || initiateAccessResponse is Resource.Error
            || retrieveAccessShardsResponse is Resource.Error
            || verifyKeyConfirmationSignature is Resource.Error
            || completeApprovershipResponse is Resource.Error

    enum class BackIconType {
        None, Back, Exit
    }
}

enum class PlanSetupUIState {
    Initial_1,
    ApproverNickname_2,
    EditApproverNickname_3,
    ApproverGettingLive_4,
    ApproverActivation_5,
    AddAlternateApprover_6,
    AccessInProgress_7,
    Completed_8
}

sealed interface PlanSetupAction {

    //Approver Setup
    data class ApproverNicknameChanged(val name: String) : PlanSetupAction
    object EditApproverNickname : PlanSetupAction
    object EditApproverAndSavePolicy : PlanSetupAction
    object InviteApprover : PlanSetupAction
    object SaveApproverAndSavePolicy : PlanSetupAction
    object GoLiveWithApprover: PlanSetupAction
    object ApproverConfirmed : PlanSetupAction

    //Back
    object BackClicked : PlanSetupAction

    //Retry
    object Retry : PlanSetupAction
}

enum class ApproverType {
    Primary, Alternate
}