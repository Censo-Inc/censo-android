package co.censo.vault.storage

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

typealias BIP39Phrases = Map<String, EncryptedBIP39>

@Serializable
data class EncryptedBIP39(
    val base64: String,
    val createdAt: Instant
)

data class BIP39(
    val words: List<String>
)