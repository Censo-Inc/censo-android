package co.censo.vault.presentation.activate_approvers

import Base58EncodedIntermediatePublicKey
import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.model.ConfirmGuardianshipApiResponse
import co.censo.shared.data.model.CreatePolicyApiResponse
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.OwnerState
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class ActivateApproversState(
    val ownerState: OwnerState = OwnerState.Initial,
    val guardians: List<Guardian> = emptyList(),
    val counter: Long = Clock.System.now().epochSeconds.div(CODE_EXPIRATION),
    val currentSecond: Int = Clock.System.now().toLocalDateTime(TimeZone.UTC).second,
    val approverCodes: Map<ParticipantId, String> = emptyMap(),
    val userResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val createPolicyResponse: Resource<CreatePolicyApiResponse> = Resource.Uninitialized,
    val confirmGuardianshipResponse: Resource<ConfirmGuardianshipApiResponse> = Resource.Uninitialized,
    val policyIntermediatePublicKey: Base58EncodedIntermediatePublicKey = Base58EncodedIntermediatePublicKey(
        ""
    ),
    val codeNotValidError: Boolean = false
) {
    companion object {
        const val CODE_EXPIRATION = 60L
    }

    val loading =
        userResponse is Resource.Loading ||
                createPolicyResponse is Resource.Loading

    val asyncError =
        userResponse is Resource.Error ||
                createPolicyResponse is Resource.Error ||
                codeNotValidError

    val countdownPercentage = 1.0f - (currentSecond.toFloat() / CODE_EXPIRATION.toFloat())
}