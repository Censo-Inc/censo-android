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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val billingClient: BillingClientWrapper
) : ViewModel() {

    var state by mutableStateOf(PaywallState())
        private set

    fun onStart() {
        viewModelScope.launch {
            ownerRepository.collectOwnerState {
                onOwnerState(it)
            }
        }

        viewModelScope.launch {
            billingClient.purchasesList.collectLatest {
                onPurchasesUpdated(
                    purchases = it,
                )
            }
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

    fun setPaywallVisibility(
        ignoreSubscriptionRequired: Boolean,
        onSuccessfulPurchase: (() -> Unit)?,
        onCancelPurchase: (() -> Unit)?
        ) {
        state = state.copy(
            ignoreSubscriptionRequired = ignoreSubscriptionRequired,
            successfulPurchaseCallback = onSuccessfulPurchase,
            cancelPurchaseCallback = onCancelPurchase
        )

        onStart()
    }

    private fun onOwnerState(ownerState: OwnerState) {
        state = state.copy(
            subscriptionStatus = ownerState.subscriptionStatus(),
            subscriptionRequired = ownerState.subscriptionRequired(),
        )

        if (ownerState.hasActiveSubscription()) {
            // make sure billing client is stopped
            if (state.billingClientReadyResource !is Resource.Uninitialized) {
                billingClient.terminateBillingConnection()
            }
        } else if (ownerState.subscriptionRequired() || state.ignoreSubscriptionRequired) {
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
            state = state.copy(submitPurchaseResource = Resource.Loading)

            val response = ownerRepository.submitPurchase(purchase.purchaseToken)

            if (response is Resource.Success) {
                ownerRepository.updateOwnerState(response.data.ownerState)

                state.successfulPurchaseCallback?.let {
                    it()
                    resetVisibility()
                }
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

    fun resetDeleteUserDialog() {
        state = state.copy(triggerDeleteUserDialog = Resource.Uninitialized)
    }

    fun deleteUser() {
        state = state.copy(
            deleteUserResource = Resource.Loading,
            triggerDeleteUserDialog = Resource.Uninitialized
        )

        viewModelScope.launch(Dispatchers.IO) {
            val response = ownerRepository.deleteUser(null)

            state = state.copy(
                deleteUserResource = response
            )

            if (response is Resource.Success) {
                state = state.copy(kickUserOut = Resource.Success(Unit))
            }
        }
    }

    fun resetVisibility() {
        state = state.copy(
            ignoreSubscriptionRequired = false,
            successfulPurchaseCallback = null,
            cancelPurchaseCallback = null
        )
    }
}
