package co.censo.shared.data.model

import Base58EncodedDevicePublicKey
import Base58EncodedPublicKey
import Base64EncodedData
import SimpleBase58PublicKey
import co.censo.shared.data.cryptography.base64Encoded
import co.censo.shared.data.cryptography.hexStringToByteArray
import co.censo.shared.data.cryptography.sha256Base64
import co.censo.shared.data.cryptography.toPaddedHexString
import co.censo.shared.util.BIP39
import io.github.novacrypto.base58.Base58
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigInteger
import java.util.Base64

@Serializable
data class Import(
    val importKey: Base58EncodedPublicKey,
    val timestamp: Long,
    val signature: Base64EncodedData,
    val name: String
) {

    companion object {
        fun fromDeeplink(
            importKey: String,
            timestamp: String,
            signature: String,
            name: String,
        ): Import {

            return Import(
                importKey = SimpleBase58PublicKey(importKey),
                timestamp = timestamp.toLong(),
                signature = Base64.getUrlDecoder().decode(signature).base64Encoded(),
                name = name
            )
        }
    }

    fun channel(): String {
        val importBytes = Base58.base58Decode(importKey.value)

        return Base64.getUrlEncoder().encodeToString(importBytes.sha256Base64().bytes)
    }
}

@Serializable
sealed class ImportState {
    @Serializable
    @SerialName("Initial")
    object Initial : ImportState()

    @Serializable
    @SerialName("Accepted")
    data class Accepted(
        val ownerDeviceKey: Base58EncodedDevicePublicKey,
        val ownerProof: Base64EncodedData,
        val acceptedAt: Instant,
    ) : ImportState()

    @Serializable
    @SerialName("Completed")
    data class Completed(
        val encryptedData: Base64EncodedData,
    ) : ImportState()
}

@Serializable
data class GetImportEncryptedDataApiResponse(
    val importState: ImportState,
)

@Serializable
data class OwnerProof(
    val signature: Base64EncodedData,
)

@Serializable
@JvmInline
value class BinaryPhrase(val value: String) {
    fun bigInt() = BigInteger(value, 16)

    init {
        try {
            val data = value.hexStringToByteArray()
            if (data.size != 32) throw Exception("Invalid Binary Phrase")
        } catch (e: Exception) {
            throw Exception("Invalid Binary Phrase")
        }
    }

    constructor(bigInteger: BigInteger) : this(bigInteger.toByteArray().toPaddedHexString(32))
}

@Serializable
data class ImportedPhrase(
    val binaryPhrase: BinaryPhrase,
    val language: BIP39.WordListLanguage,
    val label: String? = null
)