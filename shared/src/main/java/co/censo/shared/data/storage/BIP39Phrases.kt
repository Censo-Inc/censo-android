package co.censo.shared.data.storage

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

typealias BIP39Phrases = Map<String, EncryptedBIP39>

fun BIP39Phrases.toJson() = Json.encodeToString(this)
fun BIP39Phrases.fromJSON(json: String): BIP39Phrases = Json.decodeFromString(json)

@Serializable
data class EncryptedBIP39(
    val base64: String,
    val createdAt: Instant
)