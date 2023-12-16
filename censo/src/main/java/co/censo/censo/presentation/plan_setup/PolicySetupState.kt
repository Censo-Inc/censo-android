package co.censo.censo.presentation.plan_setup

import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.model.CreatePolicySetupApiResponse
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.OwnerState
import kotlinx.datetime.Clock


data class PolicySetupState(
    val ownerState: OwnerState.Ready? = null,

    // restored approvers state
    val ownerApprover: Approver.ProspectApprover? = null,
    val primaryApprover: Approver.ProspectApprover? = null,
    val alternateApprover: Approver.ProspectApprover? = null,

    // Screen in Plan Setup Flow
    val policySetupAction: PolicySetupAction = PolicySetupAction.AddApprovers,
    val policySetupUIState: PolicySetupUIState = PolicySetupUIState.Initial_1,

    // inviting approver
    val editedNickname: String = "",

    // totp
    val secondsLeft: Int = 0,
    val counter: Long = Clock.System.now().epochSeconds.div(TotpGenerator.CODE_EXPIRATION),
    val approverCodes: Map<ParticipantId, String> = emptyMap(),


    // API Calls
    val userResponse: Resource<OwnerState> = Resource.Uninitialized,
    val createPolicySetupResponse: Resource<CreatePolicySetupApiResponse> = Resource.Uninitialized,

    // Navigation
    val navigationResource: Resource<String> = Resource.Uninitialized,

    // Plan Finalization
    val replacePolicy: Resource<Unit> = Resource.Uninitialized,
) {
    companion object {
        const val APPROVER_NAME_MAX_LENGTH = 20
    }

    val activatingApprover = alternateApprover ?: primaryApprover
    val approverType = if (alternateApprover != null) ApproverType.Alternate else ApproverType.Primary

    val editedNicknameIsTooLong = editedNickname.length > APPROVER_NAME_MAX_LENGTH
    val editedNicknameValid = editedNickname.isNotEmpty() && !editedNicknameIsTooLong

    val backArrowType = when {
        policySetupUIState in listOf(
            PolicySetupUIState.ApproverActivation_5,
            PolicySetupUIState.EditApproverNickname_3
        ) -> BackIconType.Back

        policySetupUIState in listOf(
            PolicySetupUIState.ApproverGettingLive_4,
        ) -> BackIconType.Exit

        else -> BackIconType.None
    }

    val loading = userResponse is Resource.Loading
                || createPolicySetupResponse is Resource.Loading

    val asyncError = userResponse is Resource.Error
            || createPolicySetupResponse is Resource.Error

    enum class BackIconType {
        None, Back, Exit
    }
}

enum class PolicySetupUIState {
    Uninitialized_0,
    Initial_1,
    ApproverNickname_2,
    EditApproverNickname_3,
    ApproverGettingLive_4,
    ApproverActivation_5,
}

sealed interface PolicySetupScreenAction {

    //Approver Setup
    data class ApproverNicknameChanged(val name: String) : PolicySetupScreenAction
    object EditApproverNickname : PolicySetupScreenAction
    object EditApproverAndSavePolicy : PolicySetupScreenAction
    object SaveApproverAndSavePolicy : PolicySetupScreenAction
    object GoLiveWithApprover: PolicySetupScreenAction
    object ApproverConfirmed : PolicySetupScreenAction

    //Back
    object BackClicked : PolicySetupScreenAction

    //Retry
    object Retry : PolicySetupScreenAction
}

enum class ApproverType {
    Primary, Alternate
}