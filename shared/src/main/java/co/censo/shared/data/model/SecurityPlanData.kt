package co.censo.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SecurityPlanData(
    val approvers: List<Approver.SetupApprover.ExternalApprover>,
    val threshold: UInt
)
