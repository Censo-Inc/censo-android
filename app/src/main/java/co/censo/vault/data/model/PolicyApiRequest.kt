package co.censo.vault.data.model

import Base58EncodedPolicyPublicKey
import Base58EncodedPublicKey
import Base64EncodedData
import GuardianInvite
import GuardianUpdate
import ParticipantId
import kotlinx.serialization.Serializable

@Serializable
data class CreatePolicyApiRequest(
    val intermediateKey: Base58EncodedPublicKey,
    val threshold: Int,
    val guardiansToInvite: List<GuardianInvite>,
    val encryptedData: Base64EncodedData
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
)

@Serializable
data class GetPolicyApiResponse(
    val status: PolicyStatus,
    val intermediateKey: Base58EncodedPolicyPublicKey,
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
