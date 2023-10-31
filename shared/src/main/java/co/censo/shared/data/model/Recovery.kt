package co.censo.shared.data.model

import Base58EncodedDevicePublicKey
import Base64EncodedData
import ParticipantId
import VaultSecretId
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

enum class RecoveryIntent {
    AccessPhrases, ReplacePolicy
}

@Serializable
data class InitiateRecoveryApiRequest(
    val intent: RecoveryIntent,
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
    val scanResultBlob: BiometryScanResultBlob,
)

@Serializable
data class EncryptedShard(
    val participantId: ParticipantId,
    val encryptedShard: Base64EncodedData,
    val isOwnerShard: Boolean
)

data class RecoveredSeedPhrase(
    val guid: VaultSecretId,
    val label: String,
    val seedPhrase: String,
    val createdAt: Instant
)

fun List<GuardianState>.forParticipant(participantId: String): GuardianState? =
    this.find { it.participantId.value == participantId }