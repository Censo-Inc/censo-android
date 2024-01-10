package co.censo.censo.util

import Base64EncodedData
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.ApproverStatus

//region Extension Functions Mapping Approver Types
fun List<Approver.ProspectApprover>.ownerApprover(): Approver.ProspectApprover? {
    return find { it.status is ApproverStatus.OwnerAsApprover || it.status is ApproverStatus.ImplicitlyOwner }
}

fun List<Approver.ProspectApprover>.externalApprovers(): List<Approver.ProspectApprover> {
    return filter { it.status !is ApproverStatus.OwnerAsApprover && it.status !is ApproverStatus.ImplicitlyOwner }
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

fun Approver.ProspectApprover.getEntropyFromImplicitOwnerApprover(): Base64EncodedData? {
    return try {
        val implicitOwner = this.status as? ApproverStatus.ImplicitlyOwner
        return implicitOwner?.entropy ?: (this.status as? ApproverStatus.OwnerAsApprover)?.entropy
    } catch (e: Exception) {
        null
    }
}