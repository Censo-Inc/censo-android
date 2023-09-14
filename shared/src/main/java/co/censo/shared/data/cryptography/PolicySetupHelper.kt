package co.censo.shared.data.cryptography

import Base58EncodedIntermediatePublicKey
import Base58EncodedMasterPublicKey
import Base64EncodedData
import GuardianInvite
import GuardianProspect
import ParticipantId
import co.censo.shared.data.cryptography.key.EncryptionKey
import java.math.BigInteger
import java.security.PrivateKey
import java.util.Base64

class PolicySetupHelper(
    val shards: List<Point>,
    val masterEncryptionPublicKey: Base58EncodedMasterPublicKey,
    val encryptedMasterKey: Base64EncodedData,
    val threshold: Int,
    val intermediatePublicKey: Base58EncodedIntermediatePublicKey,
    val guardianInvites: List<GuardianInvite> = emptyList(),
    val deviceKey: PrivateKey
) {


    companion object {
        fun create(
            threshold: Int,
            guardians: List<GuardianProspect>,
            deviceKey: PrivateKey,
            encryptDataWithDeviceKey: (ByteArray) -> ByteArray
        ): PolicySetupHelper {
            val masterEncryptionKey = EncryptionKey.generateRandomKey()
            val intermediateEncryptionKey = EncryptionKey.generateRandomKey()

            val encryptedMasterKey = Base64.getEncoder().encodeToString(
                intermediateEncryptionKey.encrypt(
                    masterEncryptionKey.privateKeyRaw()
                )
            )

            val sharer = SecretSharer(
                secret = BigInteger(intermediateEncryptionKey.privateKeyRaw()),
                threshold = threshold,
                participants = guardians.map { it.participantId }
            )

            val guardianInvites = guardians.map { guardianProspect ->
                val shard = sharer.shards.firstOrNull { it.x == guardianProspect.participantId }

                val shardBytes = shard?.y?.toByteArray() ?: byteArrayOf()

                val encryptedShard = encryptDataWithDeviceKey(shardBytes)

                GuardianInvite(
                    name = guardianProspect.label,
                    participantId = ParticipantId(guardianProspect.participantId.toHexString()),
                    encryptedShard = Base64EncodedData(Base64.getEncoder().encodeToString(encryptedShard))
                )
            }

            return PolicySetupHelper(
                shards = sharer.shards,
                deviceKey = deviceKey,
                masterEncryptionPublicKey = Base58EncodedMasterPublicKey(masterEncryptionKey.publicExternalRepresentation().value),
                intermediatePublicKey = Base58EncodedIntermediatePublicKey(intermediateEncryptionKey.publicExternalRepresentation().value),
                encryptedMasterKey = Base64EncodedData(encryptedMasterKey),
                threshold = threshold,
                guardianInvites = guardianInvites
            )
        }
    }
}