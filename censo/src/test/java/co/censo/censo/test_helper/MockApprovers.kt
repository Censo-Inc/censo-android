package co.censo.censo.test_helper

import Base58EncodedApproverPublicKey
import Base64EncodedData
import InvitationId
import ParticipantId
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.ApproverStatus
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

val genericParticipantId = ParticipantId("AA")

//region ProspectApprovers
val prospectOwnerApprover = Approver.ProspectApprover(
    label = "Me",
    participantId = genericParticipantId,
    invitationId = InvitationId("owner_invite_id"),
    status = ApproverStatus.OwnerAsApprover(
        entropy = generateEntropy(), confirmedAt = Clock.System.now()
    ),
)

@OptIn(ExperimentalTime::class)
val prospectPrimaryApprover = Approver.ProspectApprover(
    label = "Primary",
    participantId = genericParticipantId,
    invitationId = InvitationId("primary_invite_id"),status = ApproverStatus.Confirmed(
        approverKeySignature = Base64EncodedData(base64Encoded = "AA"),
        approverPublicKey = Base58EncodedApproverPublicKey(value = "AA"),
        timeMillis = 0,
        confirmedAt = Clock.System.now().plus(
            Duration.Companion.convert(
                value = 50.0,
                sourceUnit = DurationUnit.MINUTES,
                targetUnit = DurationUnit.MINUTES
            ).toDuration(DurationUnit.MINUTES)
        )
    ),
)

@OptIn(ExperimentalTime::class)
val prospectAlternateApprover = Approver.ProspectApprover(
    label = "Alternate",
    participantId = genericParticipantId,
    invitationId = InvitationId("alt_invite_id"),status = ApproverStatus.Confirmed(
        approverKeySignature = Base64EncodedData(base64Encoded = "AA"),
        approverPublicKey = Base58EncodedApproverPublicKey(value = "AA"),
        timeMillis = 0,
        confirmedAt = Clock.System.now().plus(
            Duration.Companion.convert(
                value = 150.0,
                sourceUnit = DurationUnit.MINUTES,
                targetUnit = DurationUnit.MINUTES
            ).toDuration(DurationUnit.MINUTES)
        )
    ),
)
//endregion

//region TrustedApprovers
val trustedOwnerApprover = Approver.TrustedApprover(
    label = "Me",
    participantId = genericParticipantId,
    isOwner = true,
    attributes = ApproverStatus.Onboarded(onboardedAt = Clock.System.now())
)

@OptIn(ExperimentalTime::class)
val trustedPrimaryApprover = Approver.TrustedApprover(
    label = "Primary",
    participantId = genericParticipantId,
    isOwner = false,
    attributes = ApproverStatus.Onboarded(
        onboardedAt = Clock.System.now().plus(
            Duration.Companion.convert(
                value = 50.0,
                sourceUnit = DurationUnit.MINUTES,
                targetUnit = DurationUnit.MINUTES
            ).toDuration(DurationUnit.MINUTES)
        )
    )
)

@OptIn(ExperimentalTime::class)
val trustedAlternateApprover = Approver.TrustedApprover(
    label = "Alternate",
    participantId = genericParticipantId,
    isOwner = false,
    attributes = ApproverStatus.Onboarded(
        onboardedAt = Clock.System.now().plus(
            Duration.Companion.convert(
                value = 150.0,
                sourceUnit = DurationUnit.MINUTES,
                targetUnit = DurationUnit.MINUTES
            ).toDuration(DurationUnit.MINUTES)
        )
    )
)
//endregion

val mockTrustedApprovers : List<Approver.TrustedApprover> = listOf(trustedOwnerApprover, trustedPrimaryApprover, trustedAlternateApprover)

val mockProspectApprovers : List<Approver.ProspectApprover> = listOf(prospectOwnerApprover, prospectPrimaryApprover, prospectAlternateApprover)