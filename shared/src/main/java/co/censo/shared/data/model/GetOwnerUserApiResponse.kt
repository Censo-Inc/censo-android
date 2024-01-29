package co.censo.shared.data.model

import ApprovalId
import Base58EncodedApproverPublicKey
import Base58EncodedIntermediatePublicKey
import Base58EncodedMasterPublicKey
import Base64EncodedData
import InvitationId
import ParticipantId
import SeedPhraseId
import co.censo.shared.DeepLinkURI
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bouncycastle.util.encoders.Hex
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
data class GetOwnerUserApiResponse(
    val identityToken: IdentityToken,
    val ownerState: OwnerState,
)

@Serializable
data class GetApproverUserApiResponse(
    val identityToken: IdentityToken,
    val approverStates: List<ApproverState>,
) {
    val activeApproversCount: Int = approverStates.count { it.phase.isActiveApprover() }
}

@Serializable
sealed class ApproverStatus {

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
    ) : ApproverStatus()

    @Serializable
    @SerialName("Declined")
    object Declined : ApproverStatus()

    @Serializable
    @SerialName("Accepted")
    data class Accepted(
        val deviceEncryptedTotpSecret: Base64EncodedData,
        val acceptedAt: Instant,
    ) : ApproverStatus()

    @Serializable
    @SerialName("VerificationSubmitted")
    data class VerificationSubmitted(
        val deviceEncryptedTotpSecret: Base64EncodedData,
        val signature: Base64EncodedData,
        val timeMillis: Long,
        val approverPublicKey: Base58EncodedApproverPublicKey,
        val submittedAt: Instant,
    ) : ApproverStatus()

    @Serializable
    @SerialName("Confirmed")
    data class Confirmed(
        val approverKeySignature: Base64EncodedData, // signature of approverPublicKey + timeMillis + participantId
        val approverPublicKey: Base58EncodedApproverPublicKey,
        val timeMillis: Long,
        val confirmedAt: Instant,
    ) : ApproverStatus()

    @Serializable
    @SerialName("OwnerAsApprover")
    data class OwnerAsApprover(
        val entropy: Base64EncodedData,
        val confirmedAt: Instant,
    ) : ApproverStatus()

    @Serializable
    @SerialName("Onboarded")
    data class Onboarded(
        val onboardedAt: Instant,
    ) : ApproverStatus()
}

@Serializable
sealed class Approver {
    abstract val label: String
    abstract val participantId: ParticipantId

    @Serializable
    @SerialName("Setup")
    sealed class SetupApprover : Approver() {
        abstract override val label: String
        abstract override val participantId: ParticipantId

        @Serializable
        @SerialName("OwnerAsApprover")
        data class OwnerAsApprover(
            override val label: String,
            override val participantId: ParticipantId,
        ) : SetupApprover()

        @Serializable
        @SerialName("ExternalApprover")
        data class ExternalApprover(
            override val label: String,
            override val participantId: ParticipantId,
            val deviceEncryptedTotpSecret: Base64EncodedData,
        ) : SetupApprover()
    }

    @Serializable
    @SerialName("Prospect")
    data class ProspectApprover(
        val invitationId: InvitationId?,
        override val label: String,
        override val participantId: ParticipantId,
        val status: ApproverStatus,
    ) : Approver()

    @Serializable
    @SerialName("Trusted")
    data class TrustedApprover(
        override val label: String,
        override val participantId: ParticipantId,
        val isOwner: Boolean,
        val attributes: ApproverStatus.Onboarded,
    ) : Approver()
}

fun Approver.ProspectApprover.deeplink(): String {
    return "${DeepLinkURI.APPROVER_INVITE_URI}${this.invitationId?.value}"
}

@Serializable
data class Policy(
    val createdAt: Instant,
    val approvers: List<Approver.TrustedApprover>,
    val threshold: UInt,
    val encryptedMasterKey: Base64EncodedData,
    val intermediateKey: Base58EncodedIntermediatePublicKey,
    val approverKeysSignatureByIntermediateKey: Base64EncodedData,

    val masterKeySignature: Base64EncodedData?,
    val ownerEntropy: Base64EncodedData?,

    val owner: Approver.TrustedApprover? =
        approvers.firstOrNull { it.isOwner }
)

@Serializable
sealed class Access {
    abstract val guid: AccessId

    @Serializable
    @SerialName("AnotherDevice")
    data class AnotherDevice(
        override val guid: AccessId,
    ) : Access()

    @Serializable
    @SerialName("ThisDevice")
    class ThisDevice(
        override val guid: AccessId,
        val status: AccessStatus,
        val createdAt: Instant,
        val unlocksAt: Instant,
        val expiresAt: Instant,
        val approvals: List<AccessApproval>,
        val intent: AccessIntent,
    ) : Access()
}

@Serializable
enum class AccessStatus {
    Requested, Timelocked, Available
}

@Serializable
data class AccessApproval(
    val approvalId: ApprovalId,
    val participantId: ParticipantId,
    val status: AccessApprovalStatus,
) {
    fun deepLink(): String ="${DeepLinkURI.APPROVER_ACCESS_URI}${participantId.value}"

    fun v2Deeplink(): String = "${DeepLinkURI.APPROVER_ACCESS_V2_URI}${participantId.value}/${approvalId.value}"
}

@Serializable
enum class AccessApprovalStatus {
    Initial, WaitingForVerification, WaitingForApproval, Approved, Rejected,
}

@Serializable
data class SeedPhrase(
    val guid: SeedPhraseId,
    val seedPhraseHash: HashedValue,
    val label: String,
    val createdAt: Instant,
)

@Serializable
data class Vault(
    val seedPhrases: List<SeedPhrase>,
    val publicMasterEncryptionKey: Base58EncodedMasterPublicKey,
)

@Serializable
data class PolicySetup(
    val approvers: List<Approver.ProspectApprover>,
    val threshold: UInt? = null,
)

@Serializable
data class TimelockSetting(
    val defaultTimelockInSeconds: Long,
    val currentTimelockInSeconds: Long?,
    val disabledAt: Instant?,
)

@Serializable
sealed class AuthenticationReset {
    abstract val guid: AuthenticationResetId

    @Serializable
    @SerialName("ThisDevice")
    data class ThisDevice(
        override val guid: AuthenticationResetId,
        val status: AuthenticationResetStatus,
        val createdAt: Instant,
        val expiresAt: Instant,
        val approvals: List<AuthenticationResetApproval>,
    ) : AuthenticationReset()

    @Serializable
    @SerialName("AnotherDevice")
    data class AnotherDevice(
        override val guid: AuthenticationResetId,
    ) : AuthenticationReset()
}

@Serializable
enum class AuthenticationResetStatus {
    Requested, Approved, Completed, Expired
}

@Serializable
data class AuthenticationResetApproval(
    val guid: AuthenticationResetApprovalId,
    val participantId: ParticipantId,
    val totpSecret: AuthResetTotpSecret,
    val status: AuthenticationResetApprovalStatus,
) {
    fun v2Deeplink(): String = "${DeepLinkURI.APPROVER_AUTH_REST_V2_URI}${participantId.value}/${guid.value}"
}

@Serializable
enum class AuthenticationResetApprovalStatus {
    Initial, // owner initiated auth reset
    WaitingForTotp, // approver has opened the link and is waiting for totp from owner
    WaitingForVerification, // approver has submitted totp signature and backend is verifying it
    Approved, // approver has submitted totp signature and backend verified it successfully
    TotpVerificationFailed, // approver has submitted an invalid totp signature
    Rejected, // approver has rejected auth reset
}


@Serializable
sealed class OwnerState {
    @Serializable
    @SerialName("Initial")
    data class Initial(
        val entropy: Base64EncodedData,
        val subscriptionStatus: SubscriptionStatus,
        val subscriptionRequired: Boolean
    ) : OwnerState()

    @Serializable
    @SerialName("Ready")
    data class Ready(
        val subscriptionStatus: SubscriptionStatus,
        val policy: Policy,
        val vault: Vault,
        val unlockedForSeconds: ULong? = null,
        val access: Access?,
        val policySetup: PolicySetup?,
        val timelockSetting: TimelockSetting,
        val onboarded: Boolean,
        val canRequestAuthenticationReset: Boolean,
        val authenticationReset: AuthenticationReset?,
        val subscriptionRequired: Boolean,
    ) : OwnerState() {
        val locksAt: Instant? = unlockedForSeconds?.calculateLocksAt()

        fun hasBlockingPhraseAccessRequest(): Boolean {
            return when (access) {
                is Access.ThisDevice -> access.intent == AccessIntent.AccessPhrases && listOf(AccessStatus.Timelocked, AccessStatus.Available).contains(access.status)
                else -> false
            }
        }
    }

    fun subscriptionStatus(): SubscriptionStatus = when (this) {
        is Initial -> subscriptionStatus
        is Ready -> subscriptionStatus
    }


    fun subscriptionRequired(): Boolean = when (this) {
        is Initial -> subscriptionRequired
        is Ready -> subscriptionRequired
    }
    fun hasActiveSubscription(): Boolean = subscriptionStatus() == SubscriptionStatus.Active

    fun onboarding(): Boolean = when (this) {
        is Initial -> true
        is Ready -> !onboarded
    }

}

fun ULong?.calculateLocksAt(): Instant? {
    return this?.let {
        Clock.System.now().plus(it.toLong().toDuration(DurationUnit.SECONDS))
    }
}

@Serializable
@JvmInline
value class AccessId(val value: String)

@Serializable
@JvmInline
value class AuthenticationResetId(val value: String)

@Serializable
@JvmInline
value class AuthenticationResetApprovalId(val value: String)

@Serializable
@JvmInline
value class AuthResetTotpSecret(val value: String)

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

@Serializable
enum class SubscriptionStatus {
    None, Pending, Active, Paused
}