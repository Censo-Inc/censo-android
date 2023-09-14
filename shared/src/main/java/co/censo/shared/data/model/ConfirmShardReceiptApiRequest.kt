package co.censo.shared.data.model

import Base64EncodedData
import kotlinx.serialization.Serializable

@Serializable
data class ConfirmShardReceiptApiRequest(
    val encryptedShard: Base64EncodedData,
)