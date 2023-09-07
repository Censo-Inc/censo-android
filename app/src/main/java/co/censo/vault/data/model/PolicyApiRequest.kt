package co.censo.vault.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GuardianInvite(
    val name: String,
    val email: String,
)

@Serializable
data class CreatePolicyApiRequest(
    val intermediateKey: String, // publicKey
    val threshold: Int,
    val guardiansToInvite: List<GuardianInvite>,
)

@Serializable
data class UpdatePolicyApiRequest(
    val guardiansToInvite: List<GuardianInvite>,
)

@Serializable
data class Guardian(
    val name: String,
    val email: String,
    val status: GuardianStatus,
    val encryptedVerificationData: String?,
)

@Serializable
data class Policy(
    val status: PolicyStatus,
    val intermediateKey: String, // publicKey
    val threshold: Int,
    val guardians: List<Guardian>,
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
