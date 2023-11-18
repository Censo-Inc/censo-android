package co.censo.approver.presentation.onboarding

import Base64EncodedData
import InvitationId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.model.AcceptGuardianshipApiResponse
import co.censo.shared.data.model.GetApproverUserApiResponse
import co.censo.shared.data.model.GuardianState
import co.censo.shared.data.model.SubmitGuardianVerificationApiResponse
import co.censo.shared.presentation.cloud_storage.CloudStorageActionData

data class ApproverOnboardingState(
    // guardian state
    val guardianState: GuardianState? = null,

    // deep links data
    val invitationId: InvitationId = InvitationId(""),
    val participantId: String = "",

    // onboarding
    val verificationCode: String = "",
    val guardianEncryptionKey: EncryptionKey? = null,
    val entropy: Base64EncodedData? = null,
    val userResponse: Resource<GetApproverUserApiResponse> = Resource.Uninitialized,
    val acceptGuardianResource: Resource<AcceptGuardianshipApiResponse> = Resource.Uninitialized,
    val submitVerificationResource: Resource<SubmitGuardianVerificationApiResponse> = Resource.Uninitialized,

    // UI state
    val approverUIState: ApproverOnboardingUIState = ApproverOnboardingUIState.Loading,
    val showTopBarCancelConfirmationDialog: Boolean = false,
    val navToApproverRouting: Boolean = false,

    //Cloud Storage
    val savePrivateKeyToCloudResource: Resource<Unit> = Resource.Uninitialized,
    val retrievePrivateKeyFromCloudResource: Resource<Unit> = Resource.Uninitialized,
    val cloudStorageAction: CloudStorageActionData = CloudStorageActionData(),

    //Success/Error Message
    val onboardingMessage: Resource<OnboardingMessage> = Resource.Uninitialized,
) {
    val asyncError = userResponse is Resource.Error
            || acceptGuardianResource is Resource.Error
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
