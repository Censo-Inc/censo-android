package co.censo.censo.presentation.paywall

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.censo.billing.BillingClientWrapper
import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.repository.OwnerRepository
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val ownerStateFlow: MutableStateFlow<Resource<OwnerState>>,
    private val billingClient: BillingClientWrapper
) : ViewModel() {

    var state by mutableStateOf(PaywallState())
        private set

    fun onStart() {
        viewModelScope.launch {
            ownerStateFlow.collect { resource: Resource<OwnerState> ->
                if (resource is Resource.Success) {
                    onOwnerState(resource.data!!)
                }
            }
        }

        viewModelScope.launch {
            billingClient.purchasesList.collectLatest { onPurchasesUpdated(it) }
        }

        viewModelScope.launch {
            billingClient.productDetailsList.collectLatest { onProductsLoaded(it) }
        }

        viewModelScope.launch {
            billingClient.billingClientReadinessFlow.collectLatest { billingClientReadiness ->
                state = state.copy(billingClientReadyResource = billingClientReadiness)
            }
        }
    }

    private fun onOwnerState(ownerState: OwnerState) {
        state = state.copy(subscriptionStatus = ownerState.subscriptionStatus())

        if (ownerState.hasActiveSubscription()) {
            // make sure billing client is stopped
            if (state.billingClientReadyResource !is Resource.Uninitialized) {
                billingClient.terminateBillingConnection()
            }
        } else {
            startBillingConnection()
        }
    }

    private fun startBillingConnection() {
        if (state.billingClientReadyResource is Resource.Uninitialized) {
            billingClient.startBillingConnection()
        }
    }

    fun restartBillingConnection() {
        billingClient.terminateBillingConnection()
        billingClient.startBillingConnection()
    }

    private fun onProductsLoaded(products: List<ProductDetails>) {
        state = state.copy(products = products)

        if (products.isNotEmpty()) {
            val subscription = state.products.first()

            val productId = subscription.productId
            val offerDetails = subscription.subscriptionOfferDetails
                ?.find { it.offerId == BillingClientWrapper.FREE_TRIAL_OFFER_ID }
                ?: subscription.subscriptionOfferDetails!!.first()

            val offerToken = offerDetails.offerToken

            val trialPhrase = offerDetails.pricingPhases.pricingPhaseList.find { it.priceAmountMicros == 0L }
            val paidPhase = offerDetails.pricingPhases.pricingPhaseList.first { it.priceAmountMicros != 0L }

            state = state.copy(
                subscriptionOffer = SubscriptionOffer(
                    productId = productId,
                    offerToken = offerToken,
                    formattedPrice = paidPhase.formattedPrice,
                    billingPeriodISO8601 = paidPhase.billingPeriod,
                    feeTrialPeriodISO8601 = trialPhrase?.billingPeriod
                )
            )
        } else {
            state = state.copy(subscriptionOffer = null)
        }
    }

    private suspend fun onPurchasesUpdated(purchases: List<Purchase>) {
        state = state.copy(purchases = purchases)

        purchases.map { purchase ->
            state = state.copy(submitPurchaseResource = Resource.Loading())

            val response = ownerRepository.submitPurchase(purchase.purchaseToken)

            if (response is Resource.Success) {
                onOwnerState(response.map { it.ownerState }.data!!)
            }

            state = state.copy(submitPurchaseResource = response)
        }
    }

    fun resubmitPurchases() {
        state = state.copy(submitPurchaseResource = Resource.Uninitialized)

        viewModelScope.launch {
            onPurchasesUpdated(state.purchases)
        }
    }

    fun startPurchaseFlow(activity: Activity, subscriptionOffer: SubscriptionOffer) {
        billingClient.launchBillingFlow(
            activity,
            subscriptionOffer.productId,
            subscriptionOffer.offerToken
        )
    }

    fun restorePurchase() {
        onStart()
    }

    fun logout() {
        viewModelScope.launch {
            ownerRepository.signUserOut()
            state = state.copy(kickUserOut = Resource.Success(Unit))
        }
    }

    fun reset() {
        if (state.billingClientReadyResource !is Resource.Uninitialized) {
            billingClient.terminateBillingConnection()
        }
        state = PaywallState()
    }
}
