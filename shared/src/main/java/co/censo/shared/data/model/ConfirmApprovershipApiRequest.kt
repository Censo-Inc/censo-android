package co.censo.shared.data.model

import Base64EncodedData
import kotlinx.serialization.Serializable

@Serializable
data class ConfirmApprovershipApiRequest(
    val keyConfirmationSignature: Base64EncodedData,
    val keyConfirmationTimeMillis: Long,
)

@Serializable
data class ConfirmApprovershipApiResponse(
    val ownerState: OwnerState,
)

@Serializable
data class RejectApproverVerificationApiResponse(
    val ownerState: OwnerState,
)