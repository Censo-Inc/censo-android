package co.censo.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SubmitBiometryVerificationApiRequest(
    val biometryData: FacetecBiometry
)

@Serializable
data class SubmitBiometryVerificationApiResponse(
    val scanResultBlob: BiometryScanResultBlob
)


@Serializable
data class FacetecBiometry(
    val faceScan: String,
    val auditTrailImage: String,
    val lowQualityAuditTrailImage: String
)

@Serializable
@JvmInline
value class BiometryVerificationId(val value: String)

@Serializable
@JvmInline
value class BiometryScanResultBlob(val value: String)