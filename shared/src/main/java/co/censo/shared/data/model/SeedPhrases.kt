package co.censo.shared.data.model

import Base64EncodedData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class SeedPhraseEncryptedNotes(
    val ownerApproverKeyEncryptedText: Base64EncodedData,
    val masterKeyEncryptedText: Base64EncodedData,
)

@Serializable
data class StoreSeedPhraseApiRequest(
    val encryptedSeedPhrase: Base64EncodedData,
    val seedPhraseHash: String,
    val label: String,
    val encryptedNotes: SeedPhraseEncryptedNotes? = null,
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

@Serializable
data class UpdateSeedPhraseMetaInfoApiRequest(
    val update: Update,
) {
    @Serializable
    sealed class Update {
        @Serializable
        @SerialName("SetLabel")
        data class SetLabel(val value: String) : Update()

        @Serializable
        @SerialName("SetNotes")
        data class SetNotes(val value: SeedPhraseEncryptedNotes) : Update()

        @Serializable
        @SerialName("DeleteNotes")
        data object DeleteNotes : Update()
    }
}

@Serializable
data class UpdateSeedPhraseMetaInfoApiResponse(
    val ownerState: OwnerState,
)
