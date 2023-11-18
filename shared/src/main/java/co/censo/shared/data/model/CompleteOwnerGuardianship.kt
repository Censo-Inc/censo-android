package co.censo.shared.data.model

import Base58EncodedGuardianPublicKey
import kotlinx.serialization.Serializable

@Serializable
data class CompleteOwnerGuardianshipApiRequest(
    val guardianPublicKey: Base58EncodedGuardianPublicKey,
)

@Serializable
data class CompleteOwnerGuardianshipApiResponse(
    val ownerState: OwnerState,
)