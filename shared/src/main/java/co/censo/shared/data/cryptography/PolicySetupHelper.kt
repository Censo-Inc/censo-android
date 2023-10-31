package co.censo.shared.data.cryptography

import Base58EncodedIntermediatePublicKey
import Base58EncodedMasterPublicKey
import Base64EncodedData
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.cryptography.key.ExternalEncryptionKey
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianShard
import co.censo.shared.data.model.GuardianStatus
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import java.math.BigInteger
import java.security.KeyPair
import java.security.PrivateKey
import java.security.interfaces.ECPrivateKey
import java.util.Base64

class PolicySetupHelper(
    val masterEncryptionPublicKey: Base58EncodedMasterPublicKey,
    val encryptedMasterKey: Base64EncodedData,
    val threshold: UInt,
    val intermediatePublicKey: Base58EncodedIntermediatePublicKey,
    val guardianShards: List<GuardianShard> = emptyList(),
    val signatureByPreviousIntermediateKey: Base64EncodedData?
) {


    companion object {
        fun create(
            threshold: UInt,
            guardians: List<Guardian.ProspectGuardian>,
            masterEncryptionKey: EncryptionKey,
            previousIntermediateKey: PrivateKey? = null,
        ): PolicySetupHelper {
            val intermediateEncryptionKey = EncryptionKey.generateRandomKey()

            val encryptedMasterKey = intermediateEncryptionKey.encrypt(
                masterEncryptionKey.privateKeyRaw()
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
                    encryptedShard = encryptedShard.base64Encoded()
                )
            }

            val signatureByPreviousIntermediateKey = previousIntermediateKey?.let {
                val privateKey = it as ECPrivateKey
                val publicKey = ECPublicKeyDecoder.getPublicKeyFromPrivateKey(privateKey)
                val encryptionKey = EncryptionKey(KeyPair(publicKey, privateKey))

                encryptionKey.sign(
                    data = (intermediateEncryptionKey.publicExternalRepresentation().ecPublicKey as BCECPublicKey).q.getEncoded(
                        false
                    )
                ).base64Encoded()
            }

            return PolicySetupHelper(
                masterEncryptionPublicKey = Base58EncodedMasterPublicKey(masterEncryptionKey.publicExternalRepresentation().value),
                intermediatePublicKey = Base58EncodedIntermediatePublicKey(intermediateEncryptionKey.publicExternalRepresentation().value),
                encryptedMasterKey = encryptedMasterKey.base64Encoded(),
                threshold = threshold,
                guardianShards = guardianShards,
                signatureByPreviousIntermediateKey = signatureByPreviousIntermediateKey,
            )
        }
    }
}