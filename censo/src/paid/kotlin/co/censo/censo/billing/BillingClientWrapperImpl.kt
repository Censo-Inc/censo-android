package co.censo.censo.billing

import android.app.Activity
import android.content.Context
import co.censo.shared.data.Resource
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.sendError
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class BillingClientWrapperImpl @Inject constructor(val context: Context) : BillingClientWrapper() {

    private val _connectionReady = MutableStateFlow<Resource<Unit>>(Resource.Uninitialized)
    private val _productsReady = MutableStateFlow<Resource<Unit>>(Resource.Uninitialized)
    private val _purchasesReady = MutableStateFlow<Resource<Unit>>(Resource.Uninitialized)

    override val billingClientReadinessFlow: Flow<Resource<Unit>> =
        combine(_connectionReady, _productsReady, _purchasesReady) { flags: Array<Resource<Unit>> ->
            flags.firstOrNull { it is Resource.Error }
                ?: if (flags.all { it is Resource.Success }) {
                    Resource.Success(Unit)
                } else if (flags.all { it is Resource.Uninitialized }) {
                    Resource.Uninitialized
                } else {
                    Resource.Loading
                }
        }

    // after closing connection a new instance of BillingClient should be used. Therefore var.
    private var billingClient: BillingClient = newBillingClient()

    private fun newBillingClient() = BillingClient.newBuilder(context)
        .setListener { billingResult: BillingResult, purchases: MutableList<Purchase>? ->
            onPurchasesUpdated(billingResult, purchases)
        }
        .enablePendingPurchases()
        .build()

    override fun startBillingConnection() {
        _connectionReady.value = Resource.Loading

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                    queryProductDetails()

                    _connectionReady.value = Resource.Success(Unit)
                } else {
                    logBillingError("Failed to start billing connection", billingResult)

                    _connectionReady.value =
                        Resource.Error(exception = Exception(billingResult.debugMessage))
                }
            }

            override fun onBillingServiceDisconnected() {
                terminateBillingConnection()
                startBillingConnection()
            }
        })
    }

    private fun queryPurchases() {
        _purchasesReady.value = Resource.Loading

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { billingResult, purchaseList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _purchasesList.value = purchaseList
                _purchasesReady.value = Resource.Success(Unit)
            } else {
                logBillingError("Failed to query purchases", billingResult)

                _purchasesReady.value =
                    Resource.Error(exception = Exception(billingResult.debugMessage))
            }
        }
    }

    private fun queryProductDetails() {
        _productsReady.value = Resource.Loading

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(SUBSCRIPTION_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            ).build()

        billingClient.queryProductDetailsAsync(params) { billingResult: BillingResult, detailsList: MutableList<ProductDetails> ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    _productDetailsList.value = detailsList
                    _productsReady.value = Resource.Success(Unit)
                }

                else -> {
                    logBillingError("Failed to load product details", billingResult)

                    _productsReady.value =
                        Resource.Error(exception = Exception(billingResult.debugMessage))
                }
            }
        }
    }

    override fun launchBillingFlow(activity: Activity, productId: String, offerToken: String) {
        val productDetails = _productDetailsList.value.first { it.productId == productId }

        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )
        ).build()

        val billingResult = billingClient.launchBillingFlow(activity, params)

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            logBillingError("Failed to launch billing flow", billingResult)
        }
    }

    private fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _purchasesList.value = purchases ?: emptyList()
        } else {
            logBillingError("Error while listening to purchases update", billingResult)
        }
    }

    override fun terminateBillingConnection() {
        billingClient.endConnection()

        _productDetailsList.value = listOf()
        _purchasesList.value = listOf()
        _connectionReady.value = Resource.Uninitialized
        _productsReady.value = Resource.Uninitialized
        _purchasesReady.value = Resource.Uninitialized

        // on next connection attempt new instance of billing client will be used
        billingClient = newBillingClient()
    }

    private fun logBillingError(action: String, billingResult: BillingResult) {
        Exception("$action: ${BillingResponseCodeMapping.fromCode(billingResult.responseCode)} ${billingResult.debugMessage}")
            .sendError(CrashReportingUtil.BillingSubscription)
    }
}