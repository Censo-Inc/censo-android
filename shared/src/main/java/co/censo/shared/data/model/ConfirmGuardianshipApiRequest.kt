package co.censo.shared.data.model

import Base64EncodedData
import kotlinx.serialization.Serializable

@Serializable
data class ConfirmGuardianshipApiRequest(
    val keyConfirmationSignature: Base64EncodedData,
    val keyConfirmationTimeMillis: Long,
)

@Serializable
data class ConfirmGuardianshipApiResponse(
    val ownerState: OwnerState,
)