package co.censo.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubmitPurchaseApiRequest(
    val purchase: Purchase
) {
    @Serializable
    sealed class Purchase {
        @Serializable
        @SerialName("PlayStore")
        data class PlayStore(val purchaseToken: String): Purchase()
    }
}

@Serializable
data class SubmitPurchaseApiResponse(
    val ownerState: OwnerState
)