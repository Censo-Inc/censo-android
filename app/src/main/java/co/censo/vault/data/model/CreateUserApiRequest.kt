package co.censo.vault.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateUserApiRequest(
    val name: String,
)