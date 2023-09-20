package co.censo.vault.presentation.guardian_invitation

import Base58EncodedIntermediatePublicKey
import co.censo.shared.data.Resource
import co.censo.shared.data.model.CreateGuardianApiResponse
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.InviteGuardianApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.vault.presentation.guardian_invitation.GuardianInvitationViewModel.Companion.MIN_GUARDIAN_LIMIT
import okhttp3.ResponseBody

data class GuardianInvitationState(
    val threshold: Int = 0,
    val ownerState: OwnerState? = null,
    val userResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val createPolicyResponse: Resource<ResponseBody> = Resource.Uninitialized,
    val createGuardianResponse: Resource<CreateGuardianApiResponse> = Resource.Uninitialized,
    val inviteGuardianResponse: Resource<InviteGuardianApiResponse> = Resource.Uninitialized,
    val confirmShardReceiptResponse: Resource<ResponseBody> = Resource.Uninitialized,
    val policyIntermediatePublicKey: Base58EncodedIntermediatePublicKey = Base58EncodedIntermediatePublicKey(""),
    val createdGuardians: List<Guardian> = emptyList(),
    val guardianInviteStatus: GuardianInvitationStatus = GuardianInvitationStatus.ENUMERATE_GUARDIANS,
) {
    val canContinueOnboarding = (createdGuardians.size >= MIN_GUARDIAN_LIMIT && threshold > 0)
    val loading =
        userResponse is Resource.Loading || createPolicyResponse is Resource.Loading || inviteGuardianResponse is Resource.Loading
    val asyncError =
        userResponse is Resource.Error || createPolicyResponse is Resource.Error
                || inviteGuardianResponse is Resource.Error || createGuardianResponse is Resource.Error
}

enum class GuardianInvitationStatus {
    ENUMERATE_GUARDIANS, INVITE_GUARDIANS, READY
}