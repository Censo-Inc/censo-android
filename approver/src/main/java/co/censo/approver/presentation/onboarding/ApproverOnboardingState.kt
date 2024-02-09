package co.censo.approver.presentation.onboarding

import Base64EncodedData
import InvitationId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.model.AcceptApprovershipApiResponse
import co.censo.shared.data.model.GetApproverUserApiResponse
import co.censo.shared.data.model.ApproverState
import co.censo.shared.data.model.SubmitApproverVerificationApiResponse
import co.censo.shared.util.NavigationData

data class ApproverOnboardingState(
    // approver state
    val approverState: ApproverState? = null,

    // deep links data
    val invitationId: InvitationId = InvitationId(""),
    val participantId: String = "",

    // onboarding
    val verificationCode: String = "",
    val approverEncryptionKey: EncryptionKey? = null,
    val entropy: Base64EncodedData? = null,
    val userResponse: Resource<GetApproverUserApiResponse> = Resource.Uninitialized,
    val acceptApproverResource: Resource<AcceptApprovershipApiResponse> = Resource.Uninitialized,
    val submitVerificationResource: Resource<SubmitApproverVerificationApiResponse> = Resource.Uninitialized,

    // UI state
    val approverUIState: ApproverOnboardingUIState = ApproverOnboardingUIState.Loading,
    val showTopBarCancelConfirmationDialog: Boolean = false,
    val navToApproverEntrance: Boolean = false,

    //Cloud Storage
    val savePrivateKeyToCloudResource: Resource<Unit> = Resource.Uninitialized,

    //Success/Error Message
    val onboardingMessage: Resource<OnboardingMessage> = Resource.Uninitialized,

    val navigationResource: Resource<NavigationData> = Resource.Uninitialized,
) {
    val asyncError = userResponse is Resource.Error
            || acceptApproverResource is Resource.Error
            || submitVerificationResource is Resource.Error
            || savePrivateKeyToCloudResource is Resource.Error

    val showTopBar = approverUIState !is ApproverOnboardingUIState.Complete
}

enum class OnboardingMessage {
    FailedPasteLink, LinkPastedSuccessfully, LinkAccepted, CodeAccepted
}

sealed class ApproverOnboardingUIState {
    object Loading : ApproverOnboardingUIState()
    object NeedsToSaveKey : ApproverOnboardingUIState()
    object NeedsToEnterCode : ApproverOnboardingUIState()
    object WaitingForConfirmation : ApproverOnboardingUIState()
    object CodeRejected : ApproverOnboardingUIState()
    object Complete : ApproverOnboardingUIState()
}
