package co.censo.shared.data.model

import Base58EncodedGuardianPublicKey
import Base58EncodedIntermediatePublicKey
import Base58EncodedMasterPublicKey
import Base64EncodedData
import ParticipantId
import kotlinx.serialization.Serializable

@Serializable
data class CreatePolicyApiRequest(
    val masterEncryptionPublicKey: Base58EncodedMasterPublicKey,
    val encryptedMasterPrivateKey: Base64EncodedData,
    val intermediatePublicKey: Base58EncodedIntermediatePublicKey,
    val participantId: ParticipantId,
    val encryptedShard: Base64EncodedData,
    val guardianPublicKey: Base58EncodedGuardianPublicKey,
    val biometryVerificationId: BiometryVerificationId,
    val biometryData: FacetecBiometry,
)

@Serializable
data class CreatePolicyApiResponse(
    val ownerState: OwnerState,
    val scanResultBlob: BiometryScanResultBlob,
)

@Serializable
data class CreatePolicySetupApiRequest(
    val threshold: UInt,
    val guardians: List<Guardian.SetupGuardian>,
)

@Serializable
data class CreatePolicySetupApiResponse(
    val ownerState: OwnerState,
)

@Serializable
data class ReplacePolicyApiRequest(
    val masterEncryptionPublicKey: Base58EncodedMasterPublicKey,
    val encryptedMasterPrivateKey: Base64EncodedData,
    val intermediatePublicKey: Base58EncodedIntermediatePublicKey,
    val guardianShards: List<GuardianShard>,
    val signatureByPreviousIntermediateKey: Base64EncodedData,
)

@Serializable
data class GuardianShard(
    val participantId: ParticipantId,
    val encryptedShard: Base64EncodedData,
)

@Serializable
data class ReplacePolicyApiResponse(
    val ownerState: OwnerState,
)
