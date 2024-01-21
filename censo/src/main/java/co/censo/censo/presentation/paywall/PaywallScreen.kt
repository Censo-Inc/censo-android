package co.censo.censo.presentation.paywall

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import co.censo.censo.R
import co.censo.censo.presentation.Screen
import co.censo.censo.presentation.paywall.components.NoSubscriptionUI
import co.censo.censo.presentation.paywall.components.PausedSubscriptionUI
import co.censo.censo.presentation.paywall.components.PendingPaymentUI
import co.censo.shared.data.Resource
import co.censo.shared.data.model.SubscriptionStatus
import co.censo.shared.presentation.components.ConfirmationDialog
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.LargeLoading
import co.censo.shared.util.popUpToTop
import kotlinx.coroutines.delay

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun PaywallScreen(
    navController: NavController,
    viewModel: PaywallViewModel
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose { }
    }

    LaunchedEffect(key1 = state) {
        if (state.kickUserOut is Resource.Success) {
            navController.navigate(Screen.EntranceRoute.route) {
                launchSingleTop = true
                popUpToTop()
            }

            // Default state of billing screen is empty
            // Delay reset to prevent showing face scan screen for a fraction of second
            delay(500)
            viewModel.reset()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {

            state.loading || state.kickUserOut is Resource.Success -> {
                LargeLoading(
                    fullscreen = true,
                    fullscreenBackgroundColor = Color.White
                )
            }

            state.asyncError -> {
                if (state.billingClientReadyResource is Resource.Error) {
                    DisplayError(
                        errorMessage = "There was a problem connecting to the Play Store",
                        dismissAction = viewModel::restartBillingConnection,
                        retryAction = viewModel::restartBillingConnection
                    )
                } else if (state.submitPurchaseResource is Resource.Error) {
                    DisplayError(
                        errorMessage = state.submitPurchaseResource.getErrorMessage(context),
                        dismissAction = viewModel::logout,
                        retryAction = viewModel::resubmitPurchases
                    )
                }
            }

            !state.subscriptionRequired && !state.ignoreSubscriptionRequired -> {}

            else -> {
                when (state.subscriptionStatus) {

                    SubscriptionStatus.Active -> {}

                    SubscriptionStatus.None -> {
                        state.subscriptionOffer?.let {
                            NoSubscriptionUI(
                                offer = state.subscriptionOffer,
                                onContinue = { productId ->
                                    viewModel.startPurchaseFlow(
                                        context,
                                        productId
                                    )
                                },
                                onCancel = state.cancelPurchaseCallback ?: viewModel::showDeleteUserDialog,
                            )
                        }
                    }

                    SubscriptionStatus.Pending -> {
                        state.subscriptionOffer?.let {
                            PendingPaymentUI(
                                offer = it,
                                onCancel = state.cancelPurchaseCallback,
                            )
                        }
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
                                },
                                onCancel = state.cancelPurchaseCallback,
                            )
                        }
                    }
                }
            }
        }

        if (state.triggerDeleteUserDialog is Resource.Success) {
            ConfirmationDialog(
                title = stringResource(id = R.string.exit_setup),
                message = stringResource(R.string.exit_setup_details),
                onCancel = viewModel::resetDeleteUserDialog,
                onDelete = viewModel::deleteUser,
            )
        }
    }
}

