package co.censo.vault.presentation.guardian_invitation

import Base58EncodedIntermediatePublicKey
import co.censo.shared.data.Resource
import co.censo.shared.data.model.ConfirmGuardianshipApiResponse
import co.censo.shared.data.model.CreatePolicyApiResponse
import co.censo.shared.data.model.CreatePolicySetupApiResponse
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.InviteGuardianApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.vault.presentation.guardian_invitation.GuardianInvitationViewModel.Companion.MIN_GUARDIAN_LIMIT

data class GuardianInvitationState(
    val threshold: UInt = 0U,
    val ownerState: OwnerState = OwnerState.Initial,
    val guardians: List<Guardian> = emptyList(),
    val userResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val createPolicySetupResponse: Resource<CreatePolicySetupApiResponse> = Resource.Uninitialized,
    val createPolicyResponse: Resource<CreatePolicyApiResponse> = Resource.Uninitialized,
    val inviteGuardianResponse: Resource<InviteGuardianApiResponse> = Resource.Uninitialized,
    val confirmGuardianshipResponse: Resource<ConfirmGuardianshipApiResponse> = Resource.Uninitialized,
    val policyIntermediatePublicKey: Base58EncodedIntermediatePublicKey = Base58EncodedIntermediatePublicKey(""),
    val guardianInviteStatus: GuardianInvitationStatus = GuardianInvitationStatus.ENUMERATE_GUARDIANS,
    val codeNotValidError: Boolean = false
) {
    val canContinueOnboarding = guardians.size >= MIN_GUARDIAN_LIMIT

    val loading =
        userResponse is Resource.Loading ||
                createPolicySetupResponse is Resource.Loading ||
                inviteGuardianResponse is Resource.Loading ||
                createPolicyResponse is Resource.Loading

    val asyncError =
        userResponse is Resource.Error ||
                createPolicySetupResponse is Resource.Error ||
                inviteGuardianResponse is Resource.Error ||
                createPolicyResponse is Resource.Error ||
                codeNotValidError
}

enum class GuardianInvitationStatus {
    ENUMERATE_GUARDIANS, CREATE_POLICY_SETUP, INVITE_GUARDIANS, CREATE_POLICY, READY
}