package co.censo.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class VerifyContactApiRequest(
    val verificationCode: String,
)
