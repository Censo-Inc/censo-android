package co.censo.censo.presentation.paywall

import co.censo.shared.data.Resource
import co.censo.shared.data.model.SubmitPurchaseApiResponse
import co.censo.shared.data.model.SubscriptionStatus
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase


data class SubscriptionOffer(
    val productId: String,
    val offerToken: String,

    val formattedPrice: String,
    val billingPeriodISO8601: String,
    val feeTrialPeriodISO8601: String?
)

data class PaywallState(
    // play store data
    val billingClientReadyResource: Resource<Unit> = Resource.Uninitialized,
    val products: List<ProductDetails> = listOf(),
    val purchases: List<Purchase> = listOf(),

    // data
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.Active,
    val subscriptionRequired: Boolean = false,
    val ignoreSubscriptionRequired: Boolean = false,
    val subscriptionOffer: SubscriptionOffer? = null,

    // api requests
    val submitPurchaseResource: Resource<SubmitPurchaseApiResponse> = Resource.Uninitialized,

    val successfulPurchaseCallback: (() -> Unit)? = null,
    val cancelPurchaseCallback: (() -> Unit)? = null,

    // navigation
    val kickUserOut: Resource<Unit> = Resource.Uninitialized,
    val completedSubscription: Resource<Unit> = Resource.Loading,

    val triggerDeleteUserDialog: Resource<Unit> = Resource.Uninitialized,
    val deleteUserResource: Resource<Unit> = Resource.Uninitialized,
) {

    val loading = billingClientReadyResource is Resource.Loading
            || submitPurchaseResource is Resource.Loading

    val asyncError = billingClientReadyResource is Resource.Error
            || submitPurchaseResource is Resource.Error
}

