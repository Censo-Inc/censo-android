package co.censo.shared.data.model

import Base58EncodedGuardianPublicKey
import Base64EncodedData
import kotlinx.serialization.Serializable

@Serializable
data class SubmitGuardianVerificationApiRequest(
    val signature: Base64EncodedData,
    val timeMillis: Long,
    val guardianPublicKey: Base58EncodedGuardianPublicKey,
)

@Serializable
data class SubmitGuardianVerificationApiResponse(
    val guardianState: GuardianState,
)
