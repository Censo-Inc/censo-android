package co.censo.shared.data.cryptography

import Base58EncodedIntermediatePublicKey
import Base58EncodedMasterPublicKey
import Base64EncodedData
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.model.CreatePolicyApiRequest
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import java.math.BigInteger
import java.util.Base64

class PolicySetupHelper(
    val masterEncryptionPublicKey: Base58EncodedMasterPublicKey,
    val encryptedMasterKey: Base64EncodedData,
    val threshold: UInt,
    val intermediatePublicKey: Base58EncodedIntermediatePublicKey,
    val guardianShards: List<CreatePolicyApiRequest.GuardianShard> = emptyList(),
) {


    companion object {
        fun create(
            threshold: UInt,
            guardians: List<Guardian.ProspectGuardian>,
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
                participants = guardians.map { it.participantId.bigInt() }
            )

            val guardianShards = guardians.map { guardianProspect ->
                val shard = sharer.shards.firstOrNull { it.x == guardianProspect.participantId.bigInt() }
                val shardBytes = shard?.y?.toByteArray() ?: byteArrayOf()

                val guardianDevicePublicKey = (guardianProspect.status as GuardianStatus.Confirmed).guardianPublicKey.ecPublicKey

                val encryptedShard = ECIESManager.encryptMessage(
                    dataToEncrypt = shardBytes,
                    publicKeyBytes = ECPublicKeyDecoder.extractUncompressedPublicKey(
                        guardianDevicePublicKey.encoded
                    )
                )

                CreatePolicyApiRequest.GuardianShard(
                    guardianProspect.participantId,
                    Base64EncodedData(Base64.getEncoder().encodeToString(encryptedShard))
                )
            }

            return PolicySetupHelper(
                masterEncryptionPublicKey = Base58EncodedMasterPublicKey(masterEncryptionKey.publicExternalRepresentation().value),
                intermediatePublicKey = Base58EncodedIntermediatePublicKey(intermediateEncryptionKey.publicExternalRepresentation().value),
                encryptedMasterKey = Base64EncodedData(encryptedMasterKey),
                threshold = threshold,
                guardianShards = guardianShards
            )
        }
    }
}