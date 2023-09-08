package co.censo.vault.data.model

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
    val name: String,
    val participantId: ParticipantId,
    val status: GuardianStatus,
    val encryptedShard: Base64EncodedData,
    val signature: Base64EncodedData?,
    val timeMillis: Long?,
)

@Serializable
data class Policy(
    val status: PolicyStatus,
    val intermediateKey: Base58EncodedPublicKey,
    val threshold: Int,
    val guardians: List<Guardian>,
    val encryptedData: Base64EncodedData,
)

@Serializable
data class GetPoliciesApiResponse(
    val policies: List<Policy>,
)

@Serializable
enum class PolicyStatus {
    Pending,
    Active,
}

@Serializable
enum class GuardianStatus {
    Invited, // initial state
    Declined, // declined
    Accepted, // accepted (ready for code verification)
    Confirmed, // confirmed by owner
    Active, // active (they have received and stored their shard)
}
