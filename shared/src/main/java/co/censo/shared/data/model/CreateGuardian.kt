package co.censo.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateGuardianApiRequest(
    val name: String,
)

@Serializable
data class CreateGuardianApiResponse(val ownerState: OwnerState?)
