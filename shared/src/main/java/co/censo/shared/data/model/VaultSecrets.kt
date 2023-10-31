package co.censo.shared.data.model

import Base64EncodedData
import kotlinx.serialization.Serializable

@Serializable
data class StoreSecretApiRequest(
    val encryptedSeedPhrase: Base64EncodedData,
    val seedPhraseHash: String,
    val label: String,
)

@Serializable
data class StoreSecretApiResponse(
    val ownerState: OwnerState,
)

@Serializable
data class DeleteSecretApiResponse(
    val ownerState: OwnerState,
)
