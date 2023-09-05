package co.censo.vault.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FacetecResultRequest(
    val faceScan: String,
    val auditTrailImage: String,
    val lowQualityAuditTrailImage: String
)

@Serializable
data class FacetecResultResponse(
    val scanResultBlob: String
)