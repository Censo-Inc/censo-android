package co.censo.shared.data.model

import Base58EncodedGuardianPublicKey
import Base58EncodedIntermediatePublicKey
import Base58EncodedMasterPublicKey
import Base64EncodedData
import InvitationId
import ParticipantId
import VaultSecretId
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bouncycastle.util.encoders.Hex
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
data class GetUserApiResponse(
    val identityToken: IdentityToken,
    val guardianStates: List<GuardianState>,
    val ownerState: OwnerState,
)

@Serializable
sealed class GuardianStatus {
    @Serializable
    @SerialName("Initial")
    object Initial : GuardianStatus()

    @Serializable
    @SerialName("Invited")
    data class Invited(
        val invitedAt: Instant,
    ) : GuardianStatus()

    @Serializable
    @SerialName("Declined")
    object Declined : GuardianStatus()

    @Serializable
    @SerialName("Accepted")
    data class Accepted(
        val acceptedAt: Instant,
    ) : GuardianStatus()

    @Serializable
    @SerialName("VerificationSubmitted")
    data class VerificationSubmitted(
        val signature: Base64EncodedData,
        val timeMillis: Long,
        val guardianPublicKey: Base58EncodedGuardianPublicKey,
        val verificationStatus: VerificationStatus,
        val submittedAt: Instant,
    ) : GuardianStatus()

    @Serializable
    @SerialName("Confirmed")
    data class Confirmed(
        val guardianKeySignature: Base64EncodedData, // signature of guardianPublicKey + timeMillis + participantId
        val guardianPublicKey: Base58EncodedGuardianPublicKey,
        val timeMillis: Long,
        val confirmedAt: Instant,
    ) : GuardianStatus()

    @Serializable
    @SerialName("Onboarded")
    data class Onboarded(
        val guardianEncryptedShard: Base64EncodedData,
        val onboardedAt: Instant,
    ) : GuardianStatus()
}

@Serializable
sealed class Guardian {
    abstract val label: String
    abstract val participantId: ParticipantId

    @Serializable
    @SerialName("Setup")
    data class SetupGuardian(
        override val label: String,
        override val participantId: ParticipantId,
    ) : Guardian()

    @Serializable
    @SerialName("Prospect")
    data class ProspectGuardian(
        override val label: String,
        override val participantId: ParticipantId,
        val invitationId: InvitationId? = null,
        val deviceEncryptedTotpSecret: Base64EncodedData? = null,
        val status: GuardianStatus,
    ) : Guardian()

    @Serializable
    @SerialName("Trusted")
    data class TrustedGuardian(
        override val label: String,
        override val participantId: ParticipantId,
        val attributes: GuardianStatus.Onboarded,
    ) : Guardian()
}

@Serializable
data class Policy(
    val createdAt: Instant,
    val guardians: List<Guardian.TrustedGuardian>,
    val threshold: UInt,
    val encryptedMasterKey: Base64EncodedData,
    val intermediateKey: Base58EncodedIntermediatePublicKey,
)

@Serializable
data class VaultSecret(
    val guid: VaultSecretId,
    val encryptedSeedPhrase: Base64EncodedData,
    val seedPhraseHash: HashedValue,
    val label: String,
    val createdAt: Instant,
)

@Serializable
data class Vault(
    val secrets: List<VaultSecret>,
    val publicMasterEncryptionKey: Base58EncodedMasterPublicKey,
)

fun OwnerState.toSecurityPlan() : SecurityPlanData? =
    when (this) {
        is OwnerState.Initial, is OwnerState.Ready -> null
        is OwnerState.GuardianSetup ->
            SecurityPlanData(
                guardians = this.guardians.map { Guardian.SetupGuardian(it.label, it.participantId) },
                threshold = this.threshold ?: 0u
            )
    }

@Serializable
sealed class OwnerState {
    @Serializable
    @SerialName("Initial")
    object Initial : OwnerState()

    @Serializable
    @SerialName("GuardianSetup")
    data class GuardianSetup(
        val guardians: List<Guardian.ProspectGuardian>,
        val threshold: UInt? = null,
        val unlockedForSeconds: ULong? = null,
    ) : OwnerState()

    @Serializable
    @SerialName("Ready")
    data class Ready(
        val policy: Policy,
        val vault: Vault,
        val unlockedForSeconds: ULong? = null,
    ) : OwnerState() {
        val locksAt: Instant? = unlockedForSeconds?.calculateLocksAt()
    }
}

fun ULong?.calculateLocksAt(): Instant? {
    return this?.let {
        Clock.System.now().plus(it.toInt().toDuration(DurationUnit.SECONDS))

    }
}

@Serializable
@JvmInline
value class HashedValue(val value: String) {

    init {
        runCatching {
            Hex.decode(value)
        }.onFailure {
            throw IllegalArgumentException("Invalid hex string")
        }
    }
}
