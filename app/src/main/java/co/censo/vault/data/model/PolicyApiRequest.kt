package co.censo.vault.data.model

import Base58EncodedIntermediatePublicKey
import Base58EncodedMasterPublicKey
import Base64EncodedData
import GuardianInvite
import GuardianUpdate
import ParticipantId
import kotlinx.serialization.Serializable

@Serializable
data class CreatePolicyApiRequest(
    val masterEncryptionPublicKey: Base58EncodedMasterPublicKey,
    val encryptedMasterPrivateKey: Base64EncodedData,
    val intermediatePublicKey: Base58EncodedIntermediatePublicKey,
    val threshold: Int,
    val guardiansToInvite: List<GuardianInvite>,
)

@Serializable
data class UpdatePolicyApiRequest(
    val guardians: List<GuardianUpdate>,
)

@Serializable
data class Guardian(
    val id: String,
    val name: String,
    val participantId: ParticipantId,
    val status: GuardianStatus,
    val encryptedShard: Base64EncodedData,
    val signature: Base64EncodedData?,
    val timeMillis: Long?,
) {
    companion object {
        fun createNonceAndCodeData(time: Long, code: String) : ByteArray {
            return code.toByteArray() + time.toString().toByteArray()
        }
    }
}

@Serializable
data class GetPolicyApiResponse(
    val status: PolicyStatus,
    val intermediateKey: Base58EncodedIntermediatePublicKey,
    val threshold: Int,
    val guardians: List<Guardian>,
    val encryptedData: Base64EncodedData,
)

@Serializable
data class GetPoliciesApiResponse(
    val policies: List<GetPolicyApiResponse>,
)

@Serializable
enum class PolicyStatus {
    Pending,
    Active,
}
