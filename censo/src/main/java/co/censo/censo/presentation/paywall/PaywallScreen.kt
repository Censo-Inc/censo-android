package co.censo.censo.presentation.paywall

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import co.censo.censo.presentation.paywall.components.NoSubscriptionUI
import co.censo.censo.presentation.paywall.components.PausedSubscriptionUI
import co.censo.censo.presentation.paywall.components.PendingPaymentUI
import co.censo.shared.data.Resource
import co.censo.shared.data.model.SubscriptionStatus
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.LargeLoading

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun PaywallScreen(
    viewModel: PaywallViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity


    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose { }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {

            state.loading ->
                LargeLoading(
                    fullscreen = true,
                    fullscreenBackgroundColor = Color.White
                )

            state.asyncError -> {
                if (state.billingClientReadyResource is Resource.Error) {
                    DisplayError(
                        errorMessage = "Error occurred trying to connect to Play Store",
                        dismissAction = viewModel::restartBillingConnection,
                        retryAction = viewModel::restartBillingConnection
                    )
                } else if (state.submitPurchaseResource is Resource.Error) {
                    DisplayError(
                        errorMessage = "Error occurred trying while processing purchase. Please try again.",
                        dismissAction = viewModel::resubmitPurchases,
                        retryAction = viewModel::resubmitPurchases
                    )
                }
            }

            else -> {
                when (state.subscriptionStatus) {

                    SubscriptionStatus.Active -> { }

                    SubscriptionStatus.None -> {
                        state.subscriptionOffer?.let {
                            NoSubscriptionUI(
                                offer = state.subscriptionOffer,
                                onContinue = { productId ->
                                    viewModel.startPurchaseFlow(
                                        context,
                                        productId
                                    )
                                }
                            )
                        }
                    }

                    SubscriptionStatus.Pending -> {
                        PendingPaymentUI()
                    }

                    SubscriptionStatus.Paused -> {
                        state.subscriptionOffer?.let {
                            PausedSubscriptionUI(
                                offer = state.subscriptionOffer,
                                onContinue = { productId ->
                                    viewModel.startPurchaseFlow(
                                        context,
                                        productId
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

