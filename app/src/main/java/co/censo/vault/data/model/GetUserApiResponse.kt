package co.censo.vault.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GetUserApiResponse(
    val contacts: List<Contact>,
)