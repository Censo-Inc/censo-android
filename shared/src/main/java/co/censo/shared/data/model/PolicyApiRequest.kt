package co.censo.shared.data.model

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
    val guardianShards: List<GuardianShard>,
) {
    @Serializable
    data class GuardianShard(
        val participantId: ParticipantId,
        val encryptedShard: Base64EncodedData,
    )
}

@Serializable
data class CreatePolicyApiResponse(
    val ownerState: OwnerState,
)

@Serializable
data class CreatePolicySetupApiRequest(
    val threshold: UInt,
    val guardians: List<Guardian.SetupGuardian>,
    val biometryVerificationId: BiometryVerificationId,
    val biometryData: FacetecBiometry,
)

@Serializable
data class CreatePolicySetupApiResponse(
    val ownerState: OwnerState,
    val scanResultBlob: BiometryScanResultBlob,
)

@Serializable
data class GetPolicyApiResponse(
    val status: PolicyStatus,
    val intermediateKey: Base58EncodedIntermediatePublicKey,
    val threshold: Int,
    val guardians: List<Guardian>,
    val encryptedData: Base64EncodedData,
)

@Serializable
enum class PolicyStatus {
    Pending,
    Active,
}
