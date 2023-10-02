package co.censo.vault.presentation.activate_approvers

import Base58EncodedIntermediatePublicKey
import co.censo.shared.data.Resource
import co.censo.shared.data.model.ConfirmGuardianshipApiResponse
import co.censo.shared.data.model.CreatePolicyApiResponse
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.OwnerState

data class ActivateApproversState(
    val ownerState: OwnerState = OwnerState.Initial,
    val guardians: List<Guardian> = emptyList(),
    val userResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val createPolicyResponse: Resource<CreatePolicyApiResponse> = Resource.Uninitialized,
    val confirmGuardianshipResponse: Resource<ConfirmGuardianshipApiResponse> = Resource.Uninitialized,
    val policyIntermediatePublicKey: Base58EncodedIntermediatePublicKey = Base58EncodedIntermediatePublicKey(
        ""
    ),
    val codeNotValidError: Boolean = false
) {
    val loading =
        userResponse is Resource.Loading ||
                createPolicyResponse is Resource.Loading

    val asyncError =
        userResponse is Resource.Error ||
                createPolicyResponse is Resource.Error ||
                codeNotValidError
}