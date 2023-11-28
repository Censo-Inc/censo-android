package co.censo.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SignInApiRequest(
    val identityToken: IdentityToken
)

@Serializable
@JvmInline
value class JwtToken(val value: String)

@Serializable
@JvmInline
value class IdentityToken(val value: String)