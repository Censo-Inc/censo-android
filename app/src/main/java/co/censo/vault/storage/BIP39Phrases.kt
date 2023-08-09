package co.censo.vault.storage

import java.time.ZonedDateTime

typealias BIP39Phrases = Map<String, EncryptedBIP39>

data class EncryptedBIP39(
    val base64: String,
    val createdAt: ZonedDateTime
)
data class BIP39(
    val words: List<String>
)