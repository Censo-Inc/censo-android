package co.censo.shared.data.model

import Base64EncodedData
import kotlinx.serialization.Serializable

@Serializable
data class StoreSeedPhraseApiRequest(
    val encryptedSeedPhrase: Base64EncodedData,
    val seedPhraseHash: String,
    val label: String,
)

@Serializable
data class StoreSeedPhraseApiResponse(
    val ownerState: OwnerState,
)

@Serializable
data class DeleteSeedPhraseApiResponse(
    val ownerState: OwnerState,
)

@Serializable
data class GetSeedPhraseApiResponse(
    val encryptedSeedPhrase: Base64EncodedData,
)
