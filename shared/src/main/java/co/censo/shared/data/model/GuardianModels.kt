import co.censo.shared.data.cryptography.ECPublicKeyDecoder
import co.censo.shared.data.cryptography.generatePartitionId
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.cryptography.toByteArrayNoSign
import co.censo.shared.data.cryptography.toPaddedHexString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bouncycastle.util.encoders.Hex
import java.math.BigInteger
import java.util.Base64
import io.github.novacrypto.base58.Base58
import java.security.interfaces.ECPublicKey

typealias GuardianId = String

@Serializable
@JvmInline
value class Base58EncodedPrivateKey(val value: String) {
    fun bigInt() = BigInteger(1, Base58.base58Decode(this.value))

    fun toEncryptionKey() =
        EncryptionKey.generateFromPrivateKeyRaw(this.bigInt())
}

interface Base58EncodedPublicKey {
    val value: String

    val ecPublicKey: ECPublicKey
        get() = ECPublicKeyDecoder.fromBase58EncodedString(this.value)
}

@Serializable
@JvmInline
value class Base58EncodedGuardianPublicKey(override val value: String) : Base58EncodedPublicKey {
    init {
        runCatching {
            Base58.base58Decode(this.value)
        }.onFailure {
            throw IllegalArgumentException("Invalid guardian public key format")
        }
    }

    fun getBytes(): ByteArray {
        return Base58.base58Decode(this.value)
    }
}

@Serializable
@JvmInline
value class Base58EncodedDevicePublicKey(override val value: String) : Base58EncodedPublicKey {
    init {
        runCatching {
            Base58.base58Decode(value)
        }.onFailure {
            throw IllegalArgumentException("Invalid device public key format")
        }
    }
}

@Serializable
@JvmInline
value class Base58EncodedIntermediatePublicKey(override val value: String) : Base58EncodedPublicKey {
    init {
        runCatching {
            Base58.base58Decode(this.value)
        }.onFailure {
            throw IllegalArgumentException("Invalid policy public key format")
        }
    }
}

@Serializable
@JvmInline
value class Base58EncodedMasterPublicKey(override val value: String) : Base58EncodedPublicKey {
    init {
        runCatching {
            Base58.base58Decode(this.value)
        }.onFailure {
            throw IllegalArgumentException("Invalid master public key format")
        }
    }
}

@Serializable
@JvmInline
value class ParticipantId(val value: String) {
    init {
        runCatching {
            Hex.decode(value)
        }.onFailure {
            throw IllegalArgumentException("Invalid participant id format")
        }
    }

    constructor(bigInt: BigInteger): this(bigInt.toByteArrayNoSign().toPaddedHexString(32).lowercase())

    fun getBytes() = Hex.decode(value)

    fun bigInt() = BigInteger(1, Hex.decode(this.value))

    companion object {
        fun generate(): ParticipantId = ParticipantId(generatePartitionId())
    }
}


@Serializable
@JvmInline
value class Base64EncodedData(val base64Encoded: String) {
    init {
        runCatching {
            Base64.getDecoder().decode(this.base64Encoded)
        }.onFailure {
            throw IllegalArgumentException("Invalid encrypted data format")
        }
    }

    val bytes: ByteArray
        get() = Base64.getDecoder().decode(base64Encoded)
}


object BigIntegerSerializer : KSerializer<BigInteger> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BigInteger", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: BigInteger) =
        encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): BigInteger = BigInteger(decoder.decodeString())
}

@Serializable
@JvmInline
value class InvitationId(val value: String)

@Serializable
@JvmInline
value class VaultSecretId(val value: String)
