package co.censo.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FacetecBiometry(
    val faceScan: String,
    val auditTrailImage: String,
    val lowQualityAuditTrailImage: String,
    val verificationId: BiometryVerificationId? = null
)


@Serializable
@JvmInline
value class BiometryVerificationId(val value: String)

@Serializable
@JvmInline
value class BiometryScanResultBlob(val value: String)