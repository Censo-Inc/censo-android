package co.censo.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ResetLoginIdApiRequest(
    val identityToken: IdentityToken,
    val resetTokens: List<ResetToken>,
    val biometryVerificationId: BiometryVerificationId,
    val biometryData: FacetecBiometry,
    val password: Authentication.Password? = null,
)

@Serializable
data class ResetLoginIdApiResponse(
    val scanResultBlob: BiometryScanResultBlob
)

@JvmInline
@Serializable
value class ResetToken(val value: String)

@Serializable
data class RetrieveAuthTypeApiRequest(
    val resetTokens: List<ResetToken>,
)

@Serializable
data class RetrieveAuthTypeApiResponse(
    val authType: AuthType,
)

enum class AuthType {
    None,
    FaceTec,
    Password,
}

