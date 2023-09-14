package co.censo.shared.data.model

import Base58EncodedDevicePublicKey
import Base58EncodedIntermediatePublicKey
import Base64EncodedData
import ParticipantId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class GetUserApiResponse(
    val userGuid: String,
    val biometricVerificationRequired: Boolean,
    val ownerState: OwnerState?,
)

@Serializable
sealed class GuardianStatus {
    @Serializable
    @SerialName("Initial")
    data class Initial(
        val deviceEncryptedShard: Base64EncodedData,
    ) : GuardianStatus()

    @Serializable
    @SerialName("Invited")
    data class Invited(
        val deviceEncryptedShard: Base64EncodedData,
        val deviceEncryptedPin: Base64EncodedData,
        val invitedAt: Instant,
    ) : GuardianStatus()

    @Serializable
    @SerialName("Declined")
    data class Declined(
        val deviceEncryptedShard: Base64EncodedData,
    ) : GuardianStatus()

    @Serializable
    @SerialName("Accepted")
    data class Accepted(
        val deviceEncryptedShard: Base64EncodedData,
        val signature: Base64EncodedData,
        val timeMillis: Long,
        val guardianTransportPublicKey: Base58EncodedDevicePublicKey,
        val acceptedAt: Instant,
    ) : GuardianStatus()

    @Serializable
    @SerialName("Confirmed")
    data class Confirmed(
        val guardianTransportEncryptedShard: Base64EncodedData,
        val confirmedAt: Instant,
    ) : GuardianStatus()

    @Serializable
    @SerialName("Onboarded")
    data class Onboarded(
        val guardianEncryptedData: Base64EncodedData,
        val passwordHash: Base64EncodedData,
        val createdAt: Instant,
    ) : GuardianStatus()
}

@Serializable
sealed class PolicyGuardian {
    abstract val label: String
    abstract val participantId: ParticipantId

    @Serializable
    @SerialName("Prospect")
    data class ProspectGuardian(
        override val label: String,
        override val participantId: ParticipantId,
        val status: GuardianStatus,
    ) : PolicyGuardian()

    @Serializable
    @SerialName("Trusted")
    data class TrustedGuardian(
        override val label: String,
        override val participantId: ParticipantId,
        val attributes: GuardianStatus.Onboarded,
    ) : PolicyGuardian()
}

@Serializable
data class Policy<T : PolicyGuardian>(
    val createdAt: Instant,
    val guardians: List<T>,
    val threshold: UInt,
    val encryptedMasterKey: Base64EncodedData,
    val intermediateKey: Base58EncodedIntermediatePublicKey,
)

@Serializable
data class VaultSecret(
    val encryptedSeedPhrase: Base64EncodedData,
    val seedPhraseHash: Base64EncodedData,
    val label: String,
    val createdAt: Instant,
)

@Serializable
data class Vault(
    val secrets: List<VaultSecret>,
    val publicMasterEncryptionKey: Base58EncodedIntermediatePublicKey,
)

@Serializable
sealed class OwnerState {
    @Serializable
    @SerialName("PolicySetup")
    data class PolicySetup(
        val policy: Policy<PolicyGuardian.ProspectGuardian>,
        val publicMasterEncryptionKey: Base58EncodedIntermediatePublicKey,
    ) : OwnerState()

    @Serializable
    @SerialName("Ready")
    data class Ready(
        val policy: Policy<PolicyGuardian.TrustedGuardian>,
        val vault: Vault,
    ) : OwnerState()
}