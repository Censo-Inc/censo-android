package co.censo.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TimelockApiResponse(
    val ownerState: OwnerState,
)

