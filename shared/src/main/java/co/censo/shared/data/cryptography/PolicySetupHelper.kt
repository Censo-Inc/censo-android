package co.censo.shared.data.cryptography

import Base58EncodedApproverPublicKey
import Base58EncodedIntermediatePublicKey
import Base58EncodedMasterPublicKey
import Base58EncodedPrivateKey
import Base64EncodedData
import ParticipantId
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.cryptography.key.ExternalEncryptionKey
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.ApproverStatus
import co.censo.shared.data.model.ReplacePolicyApiRequest
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import java.math.BigInteger
import java.security.KeyPair
import java.security.PrivateKey
import java.security.interfaces.ECPrivateKey

class PolicySetupHelper(
    val masterEncryptionPublicKey: Base58EncodedMasterPublicKey,
    val encryptedMasterKey: Base64EncodedData,
    val threshold: UInt,
    val intermediatePublicKey: Base58EncodedIntermediatePublicKey,
    val approverShards: List<ReplacePolicyApiRequest.ApproverShard> = emptyList(),
    val approverKeysSignatureByIntermediateKey: Base64EncodedData,
    val signatureByPreviousIntermediateKey: Base64EncodedData?,
    val masterKeySignature: Base64EncodedData,
) {


    companion object {
        fun create(
            threshold: UInt,
            approvers: List<Approver.ProspectApprover>,
            masterEncryptionKey: EncryptionKey,
            previousIntermediateKey: PrivateKey? = null,
            ownerApproverEncryptedPrivateKey: ByteArray,
            entropy: Base64EncodedData,
            deviceKeyId: String,
        ): PolicySetupHelper {
            return create(
                threshold = threshold,
                approverPublicKeys = approvers.associate { approver ->
                    approver.participantId to when (val approverStatus = approver.status) {
                        is ApproverStatus.Confirmed -> approverStatus.approverPublicKey
                        is ApproverStatus.ImplicitlyOwner -> approverStatus.approverPublicKey
                        else -> throw Exception("Invalid status for policy setup")
                    }
                },
                masterEncryptionKey = masterEncryptionKey,
                previousIntermediateKey = previousIntermediateKey,
                ownerApproverEncryptedPrivateKey = ownerApproverEncryptedPrivateKey,
                entropy = entropy,
                deviceKeyId = deviceKeyId,
            )
        }

        fun create(
            threshold: UInt,
            approverPublicKeys: Map<ParticipantId, Base58EncodedApproverPublicKey>,
            masterEncryptionKey: EncryptionKey,
            previousIntermediateKey: PrivateKey? = null,
            ownerApproverEncryptedPrivateKey: ByteArray,
            entropy: Base64EncodedData,
            deviceKeyId: String,
        ): PolicySetupHelper {
            val intermediateEncryptionKey = EncryptionKey.generateRandomKey()

            val encryptedMasterKey = intermediateEncryptionKey.encrypt(
                masterEncryptionKey.privateKeyRaw()
            )

            val sharer = SecretSharer(
                secret = BigInteger(intermediateEncryptionKey.privateKeyRaw()),
                threshold = threshold.toInt(),
                participants = approverPublicKeys.keys.map { it.bigInt() }
            )

            val approverShards = approverPublicKeys.map { (participantId, approverPublicKey) ->
                val shard = sharer.shards.firstOrNull { it.x == participantId.bigInt() }
                val shardBytes = shard?.y?.toByteArray() ?: byteArrayOf()

                val encryptedShard = ExternalEncryptionKey.generateFromPublicKeyBase58(approverPublicKey)
                    .encrypt(shardBytes)

                ReplacePolicyApiRequest.ApproverShard(
                    participantId = participantId,
                    encryptedShard = encryptedShard.base64Encoded()
                )
            }

            val approverKeysSignatureByIntermediateKey = approverPublicKeys
                .values
                .sortedBy { it.value }  // natural sort order of Base58 key representation
                .map { it.getBytes() }
                .reduce { acc, key -> acc + key }
                .let { intermediateEncryptionKey.sign(it).base64Encoded() }

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

            //region Master Key Signature + Master Encryption Public Key
            val masterEncryptionPublicKey = Base58EncodedMasterPublicKey(masterEncryptionKey.publicExternalRepresentation().value)

            val decryptedOwnerApproverPrivateKey: Base58EncodedPrivateKey = ownerApproverEncryptedPrivateKey.decryptWithEntropy(
                deviceKeyId = deviceKeyId,
                entropy = entropy
            )

            val ownerApproverKeyPair = EncryptionKey.generateFromPrivateKeyRaw(decryptedOwnerApproverPrivateKey.bigInt())
            val masterKeySignature = ownerApproverKeyPair.sign(
                data = (masterEncryptionPublicKey.ecPublicKey as BCECPublicKey).q.getEncoded(
                    false
                )
            ).base64Encoded()
            //endregion

            return PolicySetupHelper(
                masterEncryptionPublicKey = masterEncryptionPublicKey,
                intermediatePublicKey = Base58EncodedIntermediatePublicKey(intermediateEncryptionKey.publicExternalRepresentation().value),
                encryptedMasterKey = encryptedMasterKey.base64Encoded(),
                threshold = threshold,
                approverShards = approverShards,
                approverKeysSignatureByIntermediateKey = approverKeysSignatureByIntermediateKey,
                signatureByPreviousIntermediateKey = signatureByPreviousIntermediateKey,
                masterKeySignature = masterKeySignature
            )
        }
    }
}