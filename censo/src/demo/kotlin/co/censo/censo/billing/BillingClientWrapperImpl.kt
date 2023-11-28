package co.censo.censo.billing

import android.app.Activity
import android.content.Context
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.generateRandom
import com.android.billingclient.api.BillingApiUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import javax.inject.Inject

class BillingClientWrapperImpl @Inject constructor(val context: Context) : BillingClientWrapper() {

    private val _billingClientReadinessFlow: MutableStateFlow<Resource<Unit>> = MutableStateFlow(
        Resource.Uninitialized
    )

    override val billingClientReadinessFlow: StateFlow<Resource<Unit>> =
        _billingClientReadinessFlow.asStateFlow()

    override fun startBillingConnection() {
        // generate product description
        _productDetailsList.value = listOf(
            BillingApiUtil.productDetails(
                """
                    {
                      "productId": "co.censo.standard.1month",
                      "type": "subs",
                      "title": "Monthly subscription (Censo)",
                      "name": "Monthly subscription",
                      "localizedIn": [
                        "en-US"
                      ],
                      "skuDetailsToken": "AEuhp4KCzmR5mQ9MNu4HFBvEJCMrSYOGXZPkQGq6o6Mun_6HvfBZ_wbb5D0oXNd20il3",
                      "subscriptionOfferDetails": [
                        {
                          "offerIdToken": "AUj/YhhwXwi3wKAAzpfo8IK4YnsXqys8VCW94FslfbLyCA/mBLiyOkOB0aYgA8ZeC1H3PmxrLW4bTJBsuBQVSi2m+3pek1pesaL1rYyKkuo8WVQ=",
                          "basePlanId": "1month",
                          "pricingPhases": [
                            {
                              "priceAmountMicros": 1990000,
                              "priceCurrencyCode": "USD",
                              "formattedPrice": "$1.99",
                              "billingPeriod": "P1M",
                              "recurrenceMode": 1
                            }
                          ],
                          "offerTags": []
                        }
                      ]
                    }
                """.trimIndent()
            )
        )

        // change state to ready
        _billingClientReadinessFlow.value = Resource.Success(Unit)
    }

    override fun launchBillingFlow(activity: Activity, productId: String, offerToken: String) {
        // generate purchase
        _purchasesList.value = listOf(
            BillingApiUtil.purchase(
                """
                    {
                      "orderId": "GPA.order-id",
                      "packageName": "co.censo.censo.debug",
                      "productId": "co.censo.standard.1month",
                      "purchaseTime": ${Clock.System.now().toEpochMilliseconds()},
                      "purchaseState": 0,
                      "purchaseToken": "app_${generateRandom(length = 32)}",
                      "quantity": 1,
                      "autoRenewing": true,
                      "acknowledged": false
                    }
                """.trimIndent()
            )
        )
    }

    override fun terminateBillingConnection() {
        // clear mocked data
        _productDetailsList.value = listOf()
        _purchasesList.value = listOf()

        // reset readiness state
        _billingClientReadinessFlow.value = Resource.Uninitialized
    }
}
