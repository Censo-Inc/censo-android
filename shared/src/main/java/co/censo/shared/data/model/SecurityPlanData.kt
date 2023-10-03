package co.censo.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SecurityPlanData(
    val guardians: List<Guardian.SetupGuardian.ExternalApprover>,
    val threshold: UInt
)
