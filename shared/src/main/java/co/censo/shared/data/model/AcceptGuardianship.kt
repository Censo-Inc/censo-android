package co.censo.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AcceptGuardianshipApiResponse(
    val guardianState: GuardianState,
)
