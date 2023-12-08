package co.censo.shared.data.model

import Base58EncodedApproverPublicKey
import Base64EncodedData
import kotlinx.serialization.Serializable

@Serializable
data class SubmitApproverVerificationApiRequest(
    val signature: Base64EncodedData,
    val timeMillis: Long,
    val approverPublicKey: Base58EncodedApproverPublicKey,
)

@Serializable
data class SubmitApproverVerificationApiResponse(
    val approverState: ApproverState,
)
