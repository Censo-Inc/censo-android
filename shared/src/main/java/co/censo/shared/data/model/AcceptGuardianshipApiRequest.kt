package co.censo.shared.data.model

import Base64EncodedData
import kotlinx.serialization.Serializable

@Serializable
data class AcceptGuardianshipApiRequest(
    val signature: Base64EncodedData,
    val timeMillis: Long,
)