package co.censo.shared.data.model

import Base58EncodedDevicePublicKey
import Base64EncodedData
import ParticipantId
import VaultSecretId
import kotlinx.serialization.Serializable

@Serializable
data class InitiateRecoveryApiRequest(
    val vaultSecretIds: List<VaultSecretId>,
)

@Serializable
data class InitiateRecoveryApiResponse(
    val ownerState: OwnerState,
)

@Serializable
data class DeleteRecoveryApiResponse(
    val ownerState: OwnerState,
)

@Serializable
data class StoreRecoveryTotpSecretApiRequest(
    val deviceEncryptedTotpSecret: Base64EncodedData,
)

@Serializable
data class StoreRecoveryTotpSecretApiResponse(
    val guardianStates: List<GuardianState>,
)

@Serializable
data class SubmitRecoveryTotpVerificationApiRequest(
    val signature: Base64EncodedData,
    val timeMillis: Long,
    val ownerDevicePublicKey: Base58EncodedDevicePublicKey,
)

@Serializable
data class SubmitRecoveryTotpVerificationApiResponse(
    val ownerState: OwnerState,
)

@Serializable
data class ApproveRecoveryApiRequest(
    val encryptedShard: Base64EncodedData,
)

@Serializable
data class ApproveRecoveryApiResponse(
    val guardianStates: List<GuardianState>,
)

@Serializable
data class RejectRecoveryApiResponse(
    val guardianStates: List<GuardianState>,
)

@Serializable
data class RetrieveRecoveryShardsApiRequest(
    val biometryVerificationId: BiometryVerificationId,
    val biometryData: FacetecBiometry,
)

@Serializable
data class RetrieveRecoveryShardsApiResponse(
    val ownerState: OwnerState,
    val encryptedShards: List<EncryptedShard>,
    val scanResultBlob: String,
)

@Serializable
data class EncryptedShard(
    val participantId: ParticipantId,
    val encryptedShard: Base64EncodedData,
)
