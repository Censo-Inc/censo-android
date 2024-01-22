package co.censo.shared.data.model

import Base58EncodedApproverPublicKey
import Base58EncodedDevicePublicKey
import Base64EncodedData
import ParticipantId
import SeedPhraseId
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

enum class AccessIntent {
    AccessPhrases, ReplacePolicy, RecoverOwnerKey
}

@Serializable
data class InitiateAccessApiRequest(
    val intent: AccessIntent,
)

@Serializable
data class InitiateAccessApiResponse(
    val ownerState: OwnerState,
)

@Serializable
data class DeleteAccessApiResponse(
    val ownerState: OwnerState,
)

@Serializable
data class StoreAccessTotpSecretApiRequest(
    val deviceEncryptedTotpSecret: Base64EncodedData,
)

@Serializable
data class StoreAccessTotpSecretApiResponse(
    val approverStates: List<ApproverState>,
)

@Serializable
data class SubmitAccessTotpVerificationApiRequest(
    val signature: Base64EncodedData,
    val timeMillis: Long,
    val ownerDevicePublicKey: Base58EncodedDevicePublicKey,
)

@Serializable
data class SubmitAccessTotpVerificationApiResponse(
    val ownerState: OwnerState,
)

@Serializable
data class ApproveAccessApiRequest(
    val encryptedShard: Base64EncodedData,
)

@Serializable
data class ApproveAccessApiResponse(
    val approverStates: List<ApproverState>,
)

@Serializable
data class RejectAccessApiResponse(
    val approverStates: List<ApproverState>,
)

@Serializable
data class RetrieveAccessShardsApiRequest(
    val biometryVerificationId: BiometryVerificationId,
    val biometryData: FacetecBiometry,
)

@Serializable
data class RetrieveAccessShardsApiResponse(
    val ownerState: OwnerState,
    val encryptedShards: List<EncryptedShard>,
    val scanResultBlob: BiometryScanResultBlob,
)

@Serializable
data class EncryptedShard(
    val participantId: ParticipantId,
    val encryptedShard: Base64EncodedData,
    val isOwnerShard: Boolean,
    val ownerEntropy: Base64EncodedData?,
    val approverPublicKey: Base58EncodedApproverPublicKey?
)

@Serializable
data class AttestationChallengeResponse(
    val challenge: Base64EncodedData,
)

data class RecoveredSeedPhrase(
    val guid: SeedPhraseId,
    val label: String,
    val seedPhrase: SeedPhraseData,
    val createdAt: Instant
)

fun List<ApproverState>.forParticipant(participantId: String): ApproverState? =
    this.find { it.participantId.value == participantId }