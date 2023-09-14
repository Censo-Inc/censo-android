package co.censo.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ContactType {
    Email,
    Phone,
}