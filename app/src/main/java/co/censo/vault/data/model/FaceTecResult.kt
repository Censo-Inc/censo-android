package co.censo.vault.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SubmitBiometryVerificationApiRequest(
    val faceScan: String,
    val auditTrailImage: String,
    val lowQualityAuditTrailImage: String
)

@Serializable
data class SubmitBiometryVerificationApiResponse(
    val scanResultBlob: String
)