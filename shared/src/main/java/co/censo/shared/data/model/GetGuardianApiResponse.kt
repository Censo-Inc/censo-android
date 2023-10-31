package co.censo.shared.data.model

import Base58EncodedDevicePublicKey
import Base58EncodedGuardianPublicKey
import Base64EncodedData
import InvitationId
import ParticipantId
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetGuardianStateApiResponse(
    val guardianState: GuardianState?,
)

@Serializable
data class GuardianState(
    val participantId: ParticipantId,
    val phase: GuardianPhase,
)

@Serializable
sealed class GuardianPhase {
    @Serializable
    @SerialName("WaitingForCode")
    data class WaitingForCode(
        val invitationId: InvitationId,
    ) : GuardianPhase()

    @Serializable
    @SerialName("WaitingForVerification")
    data class WaitingForVerification(
        val invitationId: InvitationId,
    ) : GuardianPhase()

    @Serializable
    @SerialName("VerificationRejected")
    data class VerificationRejected(
        val invitationId: InvitationId,
    ) : GuardianPhase()

    @Serializable
    @SerialName("Complete")
    object Complete : GuardianPhase()

    @Serializable
    @SerialName("RecoveryRequested")
    data class RecoveryRequested(
        val createdAt: Instant,
        val recoveryPublicKey: Base58EncodedGuardianPublicKey,
    ) : GuardianPhase()

    @Serializable
    @SerialName("RecoveryVerification")
    data class RecoveryVerification(
        val createdAt: Instant,
        val recoveryPublicKey: Base58EncodedGuardianPublicKey,
        val encryptedTotpSecret: Base64EncodedData,
    ) : GuardianPhase()

    @Serializable
    @SerialName("RecoveryConfirmation")
    data class RecoveryConfirmation(
        val createdAt: Instant,
        val recoveryPublicKey: Base58EncodedGuardianPublicKey,
        val encryptedTotpSecret: Base64EncodedData,
        val ownerKeySignature: Base64EncodedData,
        val ownerKeySignatureTimeMillis: Long,
        val ownerPublicKey: Base58EncodedDevicePublicKey,
        val guardianEncryptedShard: Base64EncodedData,
    ) : GuardianPhase()
}

@Serializable
enum class VerificationStatus {
    NotSubmitted,
    WaitingForVerification,
    Verified,
    Rejected,
}