package co.censo.guardian.presentation.onboarding

import InvitationId
import co.censo.guardian.data.ApproverOnboardingUIState
import co.censo.guardian.presentation.home.GuardianUIState
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.model.AcceptGuardianshipApiResponse
import co.censo.shared.data.model.GetUserApiResponse
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
    val userResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val acceptGuardianResource: Resource<AcceptGuardianshipApiResponse> = Resource.Uninitialized,
    val submitVerificationResource: Resource<SubmitGuardianVerificationApiResponse> = Resource.Uninitialized,

    // UI state
    val approverUIState: ApproverOnboardingUIState = ApproverOnboardingUIState.MissingInviteCode,
    val showTopBarCancelConfirmationDialog: Boolean = false,
    val navToGuardianHome: Boolean = false,

    //Cloud Storage
    val savePrivateKeyToCloudResource: Resource<Unit> = Resource.Uninitialized,
    val cloudStorageAction: CloudStorageActionData = CloudStorageActionData(),
) {
    val loading = userResponse is Resource.Loading
            || acceptGuardianResource is Resource.Loading
            || submitVerificationResource is Resource.Loading

    val asyncError = userResponse is Resource.Error
            || acceptGuardianResource is Resource.Error
            || submitVerificationResource is Resource.Error
            || savePrivateKeyToCloudResource is Resource.Error
}
