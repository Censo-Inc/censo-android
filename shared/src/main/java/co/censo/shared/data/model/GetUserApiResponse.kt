package co.censo.shared.data.model

import Base58EncodedGuardianPublicKey
import Base58EncodedIntermediatePublicKey
import Base58EncodedMasterPublicKey
import Base64EncodedData
import InvitationId
import ParticipantId
import VaultSecretId
import co.censo.shared.DeepLinkURI
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

    fun resolveDeviceEncryptedTotpSecret() =
        when (this) {
            is Initial -> this.deviceEncryptedTotpSecret
            is Accepted -> this.deviceEncryptedTotpSecret
            is VerificationSubmitted -> this.deviceEncryptedTotpSecret
            else -> null
        }

    @Serializable
    @SerialName("Initial")
    data class Initial(
        val deviceEncryptedTotpSecret: Base64EncodedData,
    ) : GuardianStatus()

    @Serializable
    @SerialName("Declined")
    object Declined : GuardianStatus()

    @Serializable
    @SerialName("Accepted")
    data class Accepted(
        val deviceEncryptedTotpSecret: Base64EncodedData,
        val acceptedAt: Instant,
    ) : GuardianStatus()

    @Serializable
    @SerialName("VerificationSubmitted")
    data class VerificationSubmitted(
        val deviceEncryptedTotpSecret: Base64EncodedData,
        val signature: Base64EncodedData,
        val timeMillis: Long,
        val guardianPublicKey: Base58EncodedGuardianPublicKey,
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
    @SerialName("ImplicitlyOwner")
    data class ImplicitlyOwner(
        val guardianPublicKey: Base58EncodedGuardianPublicKey,
        val confirmedAt: Instant,
    ) : GuardianStatus()

    @Serializable
    @SerialName("Onboarded")
    data class Onboarded(
        val onboardedAt: Instant,
    ) : GuardianStatus()
}

@Serializable
sealed class Guardian {
    abstract val label: String
    abstract val participantId: ParticipantId

    @Serializable
    @SerialName("Setup")
    sealed class SetupGuardian : Guardian() {
        abstract override val label: String
        abstract override val participantId: ParticipantId

        @Serializable
        @SerialName("ImplicitlyOwner")
        data class ImplicitlyOwner(
            override val label: String,
            override val participantId: ParticipantId,
            val guardianPublicKey: Base58EncodedGuardianPublicKey,
        ) : SetupGuardian()

        @Serializable
        @SerialName("ExternalApprover")
        data class ExternalApprover(
            override val label: String,
            override val participantId: ParticipantId,
            val deviceEncryptedTotpSecret: Base64EncodedData,
        ) : SetupGuardian()
    }

    @Serializable
    @SerialName("Prospect")
    data class ProspectGuardian(
        val invitationId: InvitationId?,
        override val label: String,
        override val participantId: ParticipantId,
        val status: GuardianStatus,
    ) : Guardian()

    @Serializable
    @SerialName("Trusted")
    data class TrustedGuardian(
        override val label: String,
        override val participantId: ParticipantId,
        val isOwner: Boolean,
        val attributes: GuardianStatus.Onboarded,
    ) : Guardian()
}

fun Guardian.ProspectGuardian.deeplink(): String {
    return "${DeepLinkURI.APPROVER_INVITE_URI}${this.invitationId?.value}"
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
sealed class Recovery {
    abstract val guid: RecoveryId

    @Serializable
    @SerialName("AnotherDevice")
    data class AnotherDevice(
        override val guid: RecoveryId,
    ) : Recovery()

    @Serializable
    @SerialName("ThisDevice")
    class ThisDevice(
        override val guid: RecoveryId,
        val status: RecoveryStatus,
        val createdAt: Instant,
        val unlocksAt: Instant,
        val expiresAt: Instant,
        val approvals: List<Approval>,
        val intent: RecoveryIntent,
    ) : Recovery()
}

@Serializable
enum class RecoveryStatus {
    Requested, Timelocked, Available
}

@Serializable
data class Approval(
    val participantId: ParticipantId,
    val status: ApprovalStatus,
) {
    fun deepLink(): String ="${DeepLinkURI.APPROVER_ACCESS_URI}${participantId.value}"
}

@Serializable
enum class ApprovalStatus {
    Initial, WaitingForVerification, WaitingForApproval, Approved, Rejected,
}

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

@Serializable
data class GuardianSetup(
    val guardians: List<Guardian.ProspectGuardian>,
    val threshold: UInt? = null,
)

@Serializable
sealed class OwnerState {
    @Serializable
    @SerialName("Initial")
    object Initial : OwnerState()

    @Serializable
    @SerialName("Ready")
    data class Ready(
        val policy: Policy,
        val vault: Vault,
        val unlockedForSeconds: ULong? = null,
        val recovery: Recovery?,
        val guardianSetup: GuardianSetup?
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
value class RecoveryId(val value: String)

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
