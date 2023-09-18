package co.censo.vault.presentation.guardian_invitation

import Base58EncodedIntermediatePublicKey
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.InviteGuardianApiResponse
import co.censo.shared.data.model.PolicyGuardian
import co.censo.vault.presentation.guardian_invitation.GuardianInvitationViewModel.Companion.MIN_GUARDIAN_LIMIT
import okhttp3.ResponseBody

data class GuardianInvitationState(
    val threshold: Int = 0,
    val userResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val createPolicyResponse: Resource<ResponseBody> = Resource.Uninitialized,
    val inviteGuardianResponse: Resource<InviteGuardianApiResponse> = Resource.Uninitialized,
    val confirmShardReceiptResponse: Resource<ResponseBody> = Resource.Uninitialized,
    val bioPromptTrigger: Resource<Unit> = Resource.Uninitialized,
    val policyIntermediatePublicKey: Base58EncodedIntermediatePublicKey = Base58EncodedIntermediatePublicKey(""),
    val potentialGuardians: List<String> = emptyList(),
    val guardianDeepLinks: List<String> = emptyList(),
    val prospectGuardians: List<PolicyGuardian.ProspectGuardian> = emptyList(),
    val guardianInviteStatus: GuardianInvitationStatus = GuardianInvitationStatus.CREATE_POLICY,
    val incorrectPinCode: Boolean = false
) {
    val canContinueOnboarding = (potentialGuardians.size >= MIN_GUARDIAN_LIMIT && threshold > 0)
    val loading =
        userResponse is Resource.Loading || createPolicyResponse is Resource.Loading || inviteGuardianResponse is Resource.Loading
    val asyncError =
        userResponse is Resource.Error || createPolicyResponse is Resource.Error
                || bioPromptTrigger is Resource.Error || inviteGuardianResponse is Resource.Error || incorrectPinCode
}

enum class GuardianInvitationStatus {
    CREATE_POLICY, POLICY_SETUP, READY
}