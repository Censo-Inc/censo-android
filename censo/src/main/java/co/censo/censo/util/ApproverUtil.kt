package co.censo.censo.util

import Base64EncodedData
import co.censo.shared.data.model.Approval
import co.censo.shared.data.model.ApprovalStatus
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.ApproverStatus

//region Extension Functions Mapping Approver.ProspectApprover Types
fun List<Approver.ProspectApprover>.ownerApprover(): Approver.ProspectApprover? {
    return find { it.status is ApproverStatus.OwnerAsApprover }
}

fun List<Approver.ProspectApprover>.externalApprovers(): List<Approver.ProspectApprover> {
    return filter { it.status !is ApproverStatus.OwnerAsApprover }
}

fun List<Approver.ProspectApprover>.confirmed(): List<Approver.ProspectApprover> {
    return externalApprovers().filter {
        it.status is ApproverStatus.Confirmed || it.status is ApproverStatus.Onboarded
    }
}

fun List<Approver.ProspectApprover>.notConfirmed(): List<Approver.ProspectApprover> {
    return externalApprovers().filter {
        it.status !is ApproverStatus.Confirmed && it.status !is ApproverStatus.Onboarded
    }
}

fun List<Approver.ProspectApprover>.primaryApprover(): Approver.ProspectApprover? {
    val externalApprovers = this.externalApprovers()

    return when {
        externalApprovers.isEmpty() -> null
        externalApprovers.size == 1 -> externalApprovers.first()
        else -> externalApprovers.confirmed().minBy {
            (it.status as ApproverStatus.Confirmed).confirmedAt
        }
    }
}

fun List<Approver.ProspectApprover>.alternateApprover(): Approver.ProspectApprover? {
    val externalApprovers = this.externalApprovers()

    return when {
        externalApprovers.isEmpty() -> null
        externalApprovers.size == 1 -> null
        else -> {
            val notConfirmed = externalApprovers.notConfirmed()
            when {
                notConfirmed.isEmpty() -> externalApprovers.confirmed()
                    .maxByOrNull { (it.status as ApproverStatus.Confirmed).confirmedAt }!!

                else -> notConfirmed.first()
            }

        }
    }
}

fun Approver.ProspectApprover.asExternalApprover(): Approver.SetupApprover.ExternalApprover {
    return Approver.SetupApprover.ExternalApprover(
        label = this.label,
        participantId = this.participantId,
        deviceEncryptedTotpSecret = this.status.resolveDeviceEncryptedTotpSecret()
            ?: Base64EncodedData("")
    )
}

fun Approver.ProspectApprover.asOwnerAsApprover(): Approver.SetupApprover.OwnerAsApprover {
    return Approver.SetupApprover.OwnerAsApprover(
        label = this.label,
        participantId = this.participantId,
    )
}
//endregion

//region Extension Functions Mapping Approver.TrustedApprover types
fun List<Approval>.isApprovedFor(approver: Approver.TrustedApprover?): Boolean {
    return any { it.participantId == approver?.participantId && it.status == ApprovalStatus.Approved }
}

fun List<Approver.TrustedApprover>.external(): List<Approver.TrustedApprover> {
    return filter { !it.isOwner }
}

fun List<Approver.TrustedApprover>.primaryApprover(): Approver.TrustedApprover? {
    return external().minByOrNull { it.attributes.onboardedAt }
}

fun List<Approver.TrustedApprover>.backupApprover(): Approver.TrustedApprover? {
    val externalApprovers = external()

    return when {
        externalApprovers.isEmpty() -> null
        externalApprovers.size == 1 -> null
        else -> externalApprovers.maxBy { it.attributes.onboardedAt }
    }
}
//endregion

fun Approver.ProspectApprover.getEntropyFromOwnerApprover(): Base64EncodedData? {
    return try {
        return (this.status as? ApproverStatus.OwnerAsApprover)?.entropy
    } catch (e: Exception) {
        null
    }
}