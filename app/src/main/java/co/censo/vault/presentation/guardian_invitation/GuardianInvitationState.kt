package co.censo.vault.presentation.guardian_invitation

import co.censo.vault.data.Resource
import co.censo.vault.data.model.GetUserApiResponse
import co.censo.vault.presentation.guardian_invitation.GuardianInvitationViewModel.Companion.MIN_GUARDIAN_LIMIT
import okhttp3.ResponseBody

data class GuardianInvitationState(
    val threshold: Int = 0,
    val userResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val createPolicyResponse: Resource<ResponseBody> = Resource.Uninitialized,
    val bioPromptTrigger: Resource<Unit> = Resource.Uninitialized,
    val potentialGuardians: List<String> = emptyList(),
    val guardianDeepLinks: List<String> = emptyList(),
    val guardianInviteStatus: GuardianInvitationStatus = GuardianInvitationStatus.CREATE_POLICY,
) {
    val canContinueOnboarding = (potentialGuardians.size >= MIN_GUARDIAN_LIMIT && threshold > 0)
    val loading = userResponse is Resource.Loading || createPolicyResponse is Resource.Loading
    val asyncError = userResponse is Resource.Error || createPolicyResponse is Resource.Error || bioPromptTrigger is Resource.Error
}

enum class GuardianInvitationStatus {
    CREATE_POLICY, POLICY_SETUP, READY
}