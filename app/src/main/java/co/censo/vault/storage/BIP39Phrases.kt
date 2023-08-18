package co.censo.vault.storage

import co.censo.vault.ZonedDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

typealias BIP39Phrases = Map<String, EncryptedBIP39>

data class Vault(
    val biP39Phrases: Map<String, EncryptedBIP39>
)
@Serializable
data class EncryptedBIP39(
    val base64: String,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val createdAt: ZonedDateTime
)
data class BIP39(
    val words: List<String>
)