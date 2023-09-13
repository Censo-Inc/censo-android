package co.censo.shared.data.model

import co.censo.shared.data.model.ContactType
import kotlinx.serialization.Serializable

@Serializable
data class CreateContactApiRequest(
    val contactType: ContactType,
    val value: String,
)

@Serializable
data class CreateContactApiResponse(
    val verificationId: String,
)

@Serializable
data class CreateUserApiResponse(
    val verificationId: String,
)