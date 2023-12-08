package co.censo.shared.data.model

import Base58EncodedApproverPublicKey
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
    val approverPublicKey: Base58EncodedApproverPublicKey,
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
    val approvers: List<Approver.SetupApprover>,
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
    val approverShards: List<ApproverShard>,
    val signatureByPreviousIntermediateKey: Base64EncodedData,
)

@Serializable
data class ApproverShard(
    val participantId: ParticipantId,
    val encryptedShard: Base64EncodedData,
)

@Serializable
data class ReplacePolicyApiResponse(
    val ownerState: OwnerState,
)
