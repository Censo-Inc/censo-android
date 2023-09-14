package co.censo.shared.data.model

import Base58EncodedIntermediatePublicKey
import Base64EncodedData
import ParticipantId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetGuardianStateApiResponse(
    val guardianState: GuardianState?,
)

@Serializable
data class GuardianState(
    val intermediatePublicKey: Base58EncodedIntermediatePublicKey,
    val participantId: ParticipantId,
    val phase: GuardianPhase,
)

@Serializable
sealed class GuardianPhase {
    @Serializable
    @SerialName("WaitingForCode")
    object WaitingForCode : GuardianPhase()

    @Serializable
    @SerialName("WaitingForShard")
    object WaitingForShard : GuardianPhase()

    @Serializable
    @SerialName("ShardReceived")
    data class ShardReceived(
        val shard: Base64EncodedData,
        val passwordHash: Base64EncodedData,
    ) : GuardianPhase()

    @Serializable
    @SerialName("Complete")
    object Complete : GuardianPhase()
}