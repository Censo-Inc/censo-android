package co.censo.shared.data.model

import Base58EncodedDevicePublicKey
import Base58EncodedApproverPublicKey
import Base64EncodedData
import InvitationId
import ParticipantId
import LoginIdResetToken
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApproverState(
    val participantId: ParticipantId,
    val phase: ApproverPhase,
    val invitationId: InvitationId? = null,
    val ownerLabel: String? = null,
    val ownerLoginIdResetToken: LoginIdResetToken? = null,
)

@Serializable
sealed class ApproverPhase {

    fun isAccessPhase() =
        this is  AccessRequested || this is AccessVerification || this is AccessConfirmation

    fun isOnboardingPhase() =
        this is WaitingForCode || this is WaitingForVerification || this is VerificationRejected

    fun isActiveApprover() =
        this is Complete || this.isAccessPhase()

    fun isAuthResetPhase() = this is AuthenticationResetRequested ||
            this is AuthenticationResetWaitingForCode ||
            this is AuthenticationResetVerificationRejected

    @Serializable
    @SerialName("WaitingForCode")
    data class WaitingForCode(
        val entropy: Base64EncodedData,
    ) : ApproverPhase()

    @Serializable
    @SerialName("WaitingForVerification")
    object WaitingForVerification : ApproverPhase()

    @Serializable
    @SerialName("VerificationRejected")
    data class VerificationRejected(
        val entropy: Base64EncodedData,
    ) : ApproverPhase()

    @Serializable
    @SerialName("Complete")
    object Complete : ApproverPhase()

    @Serializable
    @SerialName("AccessRequested")
    data class AccessRequested(
        val createdAt: Instant,
        val accessPublicKey: Base58EncodedApproverPublicKey,
    ) : ApproverPhase()

    @Serializable
    @SerialName("AccessVerification")
    data class AccessVerification(
        val createdAt: Instant,
        val accessPublicKey: Base58EncodedApproverPublicKey,
        val encryptedTotpSecret: Base64EncodedData,
    ) : ApproverPhase()

    @Serializable
    @SerialName("AccessConfirmation")
    data class AccessConfirmation(
        val createdAt: Instant,
        val accessPublicKey: Base58EncodedApproverPublicKey,
        val encryptedTotpSecret: Base64EncodedData,
        val ownerKeySignature: Base64EncodedData,
        val ownerKeySignatureTimeMillis: Long,
        val ownerPublicKey: Base58EncodedDevicePublicKey,
        val approverEncryptedShard: Base64EncodedData,
        val approverEntropy: Base64EncodedData,
    ) : ApproverPhase()

    @Serializable
    @SerialName("AuthenticationResetRequested")
    data class AuthenticationResetRequested(
        val createdAt: Instant,
    ) : ApproverPhase()

    @Serializable
    @SerialName("AuthenticationResetWaitingForCode")
    data class AuthenticationResetWaitingForCode(
        val entropy: Base64EncodedData,
    ) : ApproverPhase()

    @Serializable
    @SerialName("AuthenticationResetVerificationRejected")
    data class AuthenticationResetVerificationRejected(
        val entropy: Base64EncodedData,
    ) : ApproverPhase()
}
