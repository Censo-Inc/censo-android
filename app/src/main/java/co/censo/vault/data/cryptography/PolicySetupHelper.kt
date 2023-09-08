package co.censo.vault.data.cryptography

import Base58EncodedPolicyPublicKey
import Base58EncodedPublicKey
import Base64EncodedData
import GuardianInvite
import GuardianProspect
import ParticipantId
import java.security.PrivateKey
import java.util.Base64

class PolicySetupHelper(
    val shards: List<Point>,
    val masterEncryptionPublicKey: Base58EncodedPublicKey,
    val encryptedMasterKey: Base64EncodedData,
    val threshold: Int,
    val intermediatePublicKey: Base58EncodedPublicKey,
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
                    masterEncryptionKey.privateKeyRaw().toByteArray()
                )
            )

            val sharer = SecretSharer(
                secret = intermediateEncryptionKey.privateKeyRaw(),
                threshold = threshold,
                participants = guardians.map { it.participantId }
            )

            val guardianInvites = guardians.map { guardianProspect ->
                val shard = sharer.shards.firstOrNull { it.x == guardianProspect.participantId }

                val shardBytes = shard?.y?.toByteArray() ?: byteArrayOf()

                val encryptedShard = encryptDataWithDeviceKey(shardBytes)

                GuardianInvite(
                    name = guardianProspect.label,
                    participantId = ParticipantId(""),
                    encryptedShard = Base64EncodedData(Base64.getEncoder().encodeToString(encryptedShard))
                )
            }

            return PolicySetupHelper(
                shards = sharer.shards,
                deviceKey = deviceKey,
                masterEncryptionPublicKey = Base58EncodedPolicyPublicKey(masterEncryptionKey.publicExternalRepresentation()),
                intermediatePublicKey = Base58EncodedPolicyPublicKey(intermediateEncryptionKey.publicExternalRepresentation()),
                encryptedMasterKey = Base64EncodedData(encryptedMasterKey),
                threshold = threshold,
                guardianInvites = guardianInvites
            )
        }
    }
}