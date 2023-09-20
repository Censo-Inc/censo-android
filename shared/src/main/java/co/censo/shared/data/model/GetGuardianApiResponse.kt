package co.censo.shared.data.model

import Base58EncodedIntermediatePublicKey
import Base64EncodedData
import InvitationId
import ParticipantId
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
    @SerialName("WaitingForConfirmation")
    data class WaitingForConfirmation(
        val invitationId: InvitationId,
        val verificationStatus: VerificationStatus,
    ) : GuardianPhase()

    @Serializable
    @SerialName("Complete")
    object Complete : GuardianPhase()
}

@Serializable
enum class VerificationStatus {
    NotSubmitted,
    WaitingForVerification,
    Verified,
    Rejected,
}