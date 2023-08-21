package co.censo.vault.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ContactType {
    Email,
    Phone,
}