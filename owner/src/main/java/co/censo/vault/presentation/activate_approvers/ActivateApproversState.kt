package co.censo.vault.presentation.activate_approvers

import Base58EncodedIntermediatePublicKey
import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.model.ConfirmGuardianshipApiResponse
import co.censo.shared.data.model.CreatePolicyApiResponse
import co.censo.shared.data.model.CreatePolicySetupApiResponse
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.OwnerState
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class ActivateApproversState(
    val ownerState: OwnerState? = null,
    val guardians: List<Guardian> = emptyList(),
    val counter: Long = Clock.System.now().epochSeconds.div(TotpGenerator.CODE_EXPIRATION),
    val currentSecond: Int = Clock.System.now().toLocalDateTime(TimeZone.UTC).second,
    val approverCodes: Map<ParticipantId, String> = emptyMap(),
    val userResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val createPolicyResponse: Resource<CreatePolicySetupApiResponse> = Resource.Uninitialized,
    val policyIntermediatePublicKey: Base58EncodedIntermediatePublicKey = Base58EncodedIntermediatePublicKey(
        ""
    ),
    val setupError: String? = null
) {
    val loading =
        (userResponse is Resource.Loading && ownerState == null) ||
                createPolicyResponse is Resource.Loading

    val asyncError =
        userResponse is Resource.Error ||
                createPolicyResponse is Resource.Error || setupError != null

    val countdownPercentage = 1.0f - (currentSecond.toFloat() / TotpGenerator.CODE_EXPIRATION.toFloat())
}