package co.censo.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SetPromoCodeApiRequest(
    val code: String,
)