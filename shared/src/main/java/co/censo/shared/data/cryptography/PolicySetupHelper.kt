package co.censo.shared.data.cryptography

import Base58EncodedIntermediatePublicKey
import Base58EncodedMasterPublicKey
import Base64EncodedData
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.cryptography.key.ExternalEncryptionKey
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianShard
import co.censo.shared.data.model.GuardianStatus
import java.math.BigInteger
import java.util.Base64

class PolicySetupHelper(
    val masterEncryptionPublicKey: Base58EncodedMasterPublicKey,
    val encryptedMasterKey: Base64EncodedData,
    val threshold: UInt,
    val intermediatePublicKey: Base58EncodedIntermediatePublicKey,
    val guardianShards: List<GuardianShard> = emptyList(),
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

            val guardianShards = guardians.map { guardian ->
                val shard = sharer.shards.firstOrNull { it.x == guardian.participantId.bigInt() }
                val shardBytes = shard?.y?.toByteArray() ?: byteArrayOf()

                val guardianPublicKey = when (val guardianStatus = guardian.status) {
                    is GuardianStatus.Confirmed -> guardianStatus.guardianPublicKey
                    is GuardianStatus.ImplicitlyOwner -> guardianStatus.guardianPublicKey
                    else -> throw Exception("Invalid status for policy setup")
                }
                val encryptedShard = ExternalEncryptionKey.generateFromPublicKeyBase58(guardianPublicKey).encrypt(shardBytes)

                GuardianShard(
                    participantId = guardian.participantId,
                    encryptedShard = Base64EncodedData(Base64.getEncoder().encodeToString(encryptedShard))
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