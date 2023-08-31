package co.censo.vault.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateContactApiRequest(
    val contactType: ContactType,
    val value: String,
)
