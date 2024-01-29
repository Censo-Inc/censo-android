package co.censo.shared.data.model

import Base64EncodedData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InitiateAuthenticationResetApiResponse(
    val ownerState: OwnerState,
)

@Serializable
data class CancelAuthenticationResetApiResponse(
    val ownerState: OwnerState,
)

@Serializable
data class AcceptAuthenticationResetRequestApiResponse(
    val approverStates: List<ApproverState>,
)

@Serializable
data class RejectAuthenticationResetRequestApiResponse(
    val approverStates: List<ApproverState>,
)

@Serializable
data class SubmitAuthenticationResetTotpVerificationApiRequest(
    val signature: Base64EncodedData,
    val timeMillis: Long,
)

@Serializable
data class SubmitAuthenticationResetTotpVerificationApiResponse(
    val approverStates: List<ApproverState>,
)

@Serializable
data class ReplaceAuthenticationApiRequest(
    val authentication: Authentication,
)

@Serializable
data class ReplaceAuthenticationApiResponse(
    val ownerState: OwnerState,
    val scanResultBlob: BiometryScanResultBlob?
)

@Serializable
sealed class Authentication {
    @Serializable
    @SerialName("FacetecBiometry")
    data class FacetecBiometry(
        val faceScan: Base64EncodedData,
        val auditTrailImage: Base64EncodedData,
        val lowQualityAuditTrailImage: Base64EncodedData,
        val verificationId: BiometryVerificationId,
    ) : Authentication()

    @Serializable
    @SerialName("Password")
    data class Password(
        val cryptedPassword: Base64EncodedData,
    ) : Authentication()
}
