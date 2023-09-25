package co.censo.shared.data.cryptography

import Base58EncodedIntermediatePublicKey
import Base58EncodedMasterPublicKey
import Base64EncodedData
import GuardianInvite
import GuardianProspect
import ParticipantId
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.cryptography.key.InternalDeviceKey
import co.censo.shared.data.model.CreatePolicyApiRequest
import java.math.BigInteger
import java.security.PrivateKey
import java.util.Base64

class PolicySetupHelper(
    val shards: List<Point>,
    val masterEncryptionPublicKey: Base58EncodedMasterPublicKey,
    val encryptedMasterKey: Base64EncodedData,
    val threshold: UInt,
    val intermediatePublicKey: Base58EncodedIntermediatePublicKey,
    val guardianInvites: List<GuardianInvite> = emptyList(),
    val guardianShards: List<CreatePolicyApiRequest.GuardianShard> = emptyList(),
    val deviceKey: InternalDeviceKey
) {


    companion object {
        fun create(
            threshold: UInt,
            guardians: List<GuardianProspect>,
            deviceKey: InternalDeviceKey
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
                threshold = threshold.toInt(),
                participants = guardians.map { it.participantId }
            )

            val guardianInvites = guardians.map { guardianProspect ->
                val shard = sharer.shards.firstOrNull { it.x == guardianProspect.participantId }
                val shardBytes = shard?.y?.toByteArray() ?: byteArrayOf()
                val encryptedShard = deviceKey.encrypt(shardBytes)

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
                guardianInvites = guardianInvites,
                guardianShards = guardianInvites.map { CreatePolicyApiRequest.GuardianShard(it.participantId, it.encryptedShard) }
            )
        }
    }
}