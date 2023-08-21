package co.censo.vault.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GetUserApiResponse(
    val name: String,
    val contacts: List<Contact>,
)