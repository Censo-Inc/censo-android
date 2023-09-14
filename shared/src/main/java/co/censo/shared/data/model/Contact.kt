package co.censo.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Contact(
    val identifier: String,
    val contactType: ContactType,
    val value: String,
    val verified: Boolean,
)