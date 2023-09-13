package co.censo.shared.data.model

import co.censo.shared.data.model.ContactType
import kotlinx.serialization.Serializable

@Serializable
data class CreateUserApiRequest(
    val contactType: ContactType,
    val value: String,
)