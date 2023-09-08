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

@Serializable
data class GuardianInvite(
    val name: String,
    val participantId: ParticipantId,
    val encryptedShard: Base64EncodedData
)

@Serializable
data class GuardianUpdate(
    val id: GuardianId,
    val name: String,
    val participantId: ParticipantId,
    val encryptedShard: Base64EncodedData
)

@Serializable
data class GuardianProspect(
    val label: String,
    @Serializable(with = BigIntegerSerializer::class)
    val participantId: BigInteger
)


typealias GuardianId = String
typealias Base58EncodedPublicKey = String
typealias Base58EncodedPolicyPublicKey = String
typealias Base58EncodedDevicePublicKey = String

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
}


object BigIntegerSerializer : KSerializer<BigInteger> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BigInteger", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: BigInteger) =
        encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): BigInteger = BigInteger(decoder.decodeString())
}