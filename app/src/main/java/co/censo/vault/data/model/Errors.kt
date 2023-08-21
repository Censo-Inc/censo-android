package co.censo.vault.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val reason: ReasonCode,
    val message: String,
    val displayMessage: String = message,
)

@Serializable
data class ApiErrors(val errors: List<ApiError>)

@Serializable
enum class ReasonCode {
    UnexpectedError,
    TimeoutError,
}