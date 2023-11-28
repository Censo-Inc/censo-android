package co.censo.censo.billing

import android.app.Activity
import co.censo.shared.data.Resource
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class BillingClientWrapper {

    internal val _productDetailsList = MutableStateFlow<List<ProductDetails>>(listOf())
    internal val _purchasesList = MutableStateFlow<List<Purchase>>(listOf())

    val productDetailsList: StateFlow<List<ProductDetails>> = _productDetailsList.asStateFlow()
    val purchasesList: StateFlow<List<Purchase>> = _purchasesList.asStateFlow()

    abstract val billingClientReadinessFlow: Flow<Resource<Unit>>

    abstract fun startBillingConnection()
    abstract fun terminateBillingConnection()
    abstract fun launchBillingFlow(activity: Activity, productId: String, offerToken: String)

    companion object {
        internal const val SUBSCRIPTION_PRODUCT_ID = "co.censo.standard.1month"
        internal const val FREE_TRIAL_OFFER_ID = "7-days-free-trial"
    }
}

enum class BillingResponseCodeMapping(val code: Int) {
    SERVICE_TIMEOUT(-3),
    FEATURE_NOT_SUPPORTED(-2),
    SERVICE_DISCONNECTED(-1),
    OK(0),
    USER_CANCELED(1),
    SERVICE_UNAVAILABLE(2),
    BILLING_UNAVAILABLE(3),
    ITEM_UNAVAILABLE(4),
    DEVELOPER_ERROR(5),
    ERROR(6),
    ITEM_ALREADY_OWNED(7),
    ITEM_NOT_OWNED(8),
    NETWORK_ERROR(12);

    companion object {
        fun fromCode(code: Int): BillingResponseCodeMapping? {
            return values().firstOrNull { it.code == code }
        }
    }
}
