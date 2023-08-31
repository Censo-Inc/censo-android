import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigInteger

@Serializable
data class GuardianInvite(
    val name: String,
    val participantId: ParticipantId,
    val encryptedShard: Base64EncodedData?,
    val guardianId: String?
)

@Serializable
data class GuardianProspect(
    val label: String,
    @Serializable(with = BigIntegerSerializer::class)
    val participantId: BigInteger
)


typealias ParticipantId = String
typealias Base58EncodedPublicKey = String
typealias Base64EncodedData = String


object BigIntegerSerializer : KSerializer<BigInteger> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BigInteger", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: BigInteger) =
        encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): BigInteger = BigInteger(decoder.decodeString())
}