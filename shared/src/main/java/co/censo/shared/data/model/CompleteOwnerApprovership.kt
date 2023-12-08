package co.censo.shared.data.model

import Base58EncodedApproverPublicKey
import kotlinx.serialization.Serializable

@Serializable
data class CompleteOwnerApprovershipApiRequest(
    val approverPublicKey: Base58EncodedApproverPublicKey,
)

@Serializable
data class CompleteOwnerApprovershipApiResponse(
    val ownerState: OwnerState,
)