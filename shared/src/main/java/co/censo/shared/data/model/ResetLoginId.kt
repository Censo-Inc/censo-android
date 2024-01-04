package co.censo.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ResetLoginIdApiRequest(
    val identityToken: IdentityToken,
    val resetTokens: List<ResetToken>,
    val biometryVerificationId: BiometryVerificationId,
    val biometryData: FacetecBiometry,
)

@Serializable
data class ResetLoginIdApiResponse(
    val scanResultBlob: BiometryScanResultBlob
)

@JvmInline
@Serializable
value class ResetToken(val value: String)
