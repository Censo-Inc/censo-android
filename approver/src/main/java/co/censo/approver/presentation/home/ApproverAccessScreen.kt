package co.censo.approver.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.approver.R
import co.censo.approver.data.ApproverAccessUIState
import co.censo.approver.presentation.ApproverColors
import co.censo.approver.presentation.Screen
import co.censo.approver.presentation.components.ApproveRequest
import co.censo.approver.presentation.components.ApproverTopBar
import co.censo.approver.presentation.components.OwnerCodeVerification
import co.censo.shared.data.Resource
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.LargeLoading
import co.censo.shared.presentation.components.PostSuccessAction
import co.censo.shared.util.popCurrentDestinationFromBackStack
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproverAccessScreen(
    navController: NavController,
    viewModel: ApproverAccessViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_START
            -> {
                viewModel.onStart()
            }

            Lifecycle.Event.ON_PAUSE -> {
                viewModel.onStop()
            }

            else -> Unit
        }
    }

    LaunchedEffect(key1 = state) {
        if (state.navToApproverEntrance) {
            navController.navigate(Screen.ApproverEntranceRoute.route) {
                popCurrentDestinationFromBackStack(navController)
            }

            viewModel.resetApproverEntranceNavigationTrigger()
        }
    }

    Scaffold(
        topBar = {
            if (state.showTopBar) {
                ApproverTopBar {
                    viewModel.showCloseConfirmationDialog()
                }
            }
        },
        content = { contentPadding ->
            when {
                state.loading -> {
                    LargeLoading(
                        color = SharedColors.DefaultLoadingColor,
                        fullscreen = true
                    )
                }

                state.asyncError -> {
                    when {
                        state.userResponse is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.userResponse.getErrorMessage(context),
                                dismissAction = null,
                            ) { viewModel.retrieveApproverState(false) }
                        }

                        state.storeAccessTotpSecretResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.storeAccessTotpSecretResource.getErrorMessage(
                                    context
                                ),
                                dismissAction = { viewModel.resetStoreAccessTotpSecretResource() },
                            ) { viewModel.storeAccessTotpSecret() }
                        }

                        state.approveAccessResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.approveAccessResource.getErrorMessage(context),
                                dismissAction = { viewModel.resetApproveAccessResource() },
                            ) {
                                viewModel.resetApproveAccessResource()
                                viewModel.retrieveApproverState(false)
                            }
                        }

                        state.rejectAccessResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.rejectAccessResource.getErrorMessage(context),
                                dismissAction = { viewModel.resetRejectAccessResource() },
                            ) {
                                viewModel.resetRejectAccessResource()
                                viewModel.retrieveApproverState(false)
                            }
                        }

                        state.accessNotInProgress is Resource.Error -> {
                            DisplayError(
                                errorMessage = context.getString(R.string.access_not_in_progress),
                                dismissAction = { viewModel.resetAccessNotInProgressResource() },
                                retryAction = null
                            )
                        }

                        else -> {
                            DisplayError(
                                errorMessage = stringResource(R.string.something_went_wrong),
                                dismissAction = null,
                            ) { viewModel.retrieveApproverState(false) }
                        }

                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.weight(0.3f))

                        when (state.approverAccessUIState) {

                            ApproverAccessUIState.AccessRequested -> {
                                ApproveRequest(onContinue = viewModel::storeAccessTotpSecret)
                            }

                            ApproverAccessUIState.VerifyingToTPFromOwner,
                            ApproverAccessUIState.WaitingForToTPFromOwner -> {
                                OwnerCodeVerification(
                                    totpCode = state.accessTotp?.code,
                                    secondsLeft = state.accessTotp?.currentSecond,
                                    errorEnabled = state.ownerEnteredWrongCode
                                )
                            }

                            ApproverAccessUIState.Complete -> {
                                PostSuccessAction()
                                LaunchedEffect(state.approverAccessUIState) {
                                    delay(5000)
                                    viewModel.onTopBarCloseConfirmed()
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(0.7f))
                    }
                }
            }
        }
    )

    if (state.showTopBarCancelConfirmationDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideCloseConfirmationDialog,
            text = {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = stringResource(R.string.do_you_really_want_to_cancel),
                    color = ApproverColors.PrimaryColor,
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::onTopBarCloseConfirmed
                ) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                Button(
                    onClick = viewModel::hideCloseConfirmationDialog
                ) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }

    if (state.loadKeyFromCloudResource is Resource.Loading) {
        LargeLoading(
            color = SharedColors.DefaultLoadingColor,
            fullscreen = true,
            fullscreenBackgroundColor = Color.White
        )
    }
}