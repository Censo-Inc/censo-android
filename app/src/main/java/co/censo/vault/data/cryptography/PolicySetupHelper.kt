package co.censo.vault.data.cryptography

import Base58EncodedPublicKey
import Base64EncodedData
import GuardianInvite
import GuardianProspect
import java.security.PrivateKey
import java.util.Base64

class PolicySetupHelper(
    val shards: List<Point>,
    val masterEncryptionPublicKey: Base58EncodedPublicKey,
    val encryptedMasterKey: Base64EncodedData,
    val threshold: Int,
    val policyPublicKey: Base58EncodedPublicKey,
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
            val policyEncryptionKey = EncryptionKey.generateRandomKey()

            val encryptedMasterKey = Base64.getEncoder().encodeToString(
                policyEncryptionKey.encrypt(
                    masterEncryptionKey.privateKeyRaw().toByteArray()
                )
            )

            val sharer = SecretSharer(
                secret = policyEncryptionKey.privateKeyRaw(),
                threshold = threshold,
                participants = guardians.map { it.participantId }
            )

            val guardianInvites = guardians.map { guardianProspect ->
                val shard = sharer.shards.firstOrNull { it.x == guardianProspect.participantId }

                val shardBytes = shard?.y?.toByteArray() ?: byteArrayOf()

                val encryptedShard = encryptDataWithDeviceKey(shardBytes)

                GuardianInvite(
                    name = guardianProspect.label,
                    participantId = "",
                    encryptedShard = Base64.getEncoder().encodeToString(encryptedShard)
                )
            }

            return PolicySetupHelper(
                shards = sharer.shards,
                deviceKey = deviceKey,
                masterEncryptionPublicKey = masterEncryptionKey.publicExternalRepresentation(),
                policyPublicKey = policyEncryptionKey.publicExternalRepresentation(),
                encryptedMasterKey = encryptedMasterKey,
                threshold = threshold,
                guardianInvites = guardianInvites
            )
        }
    }
}