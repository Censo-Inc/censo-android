package co.censo.shared.data.model

import co.censo.shared.data.model.GuardianState
import kotlinx.serialization.Serializable

@Serializable
data class RegisterGuardianApiResponse(
    val guardianState: GuardianState,
)