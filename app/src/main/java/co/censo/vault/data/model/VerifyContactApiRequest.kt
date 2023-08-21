package co.censo.vault.data.model

import kotlinx.serialization.Serializable

@Serializable
data class VerifyContactApiRequest(
    val verificationCode: String,
)
