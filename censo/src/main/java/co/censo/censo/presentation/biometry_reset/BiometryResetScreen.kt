package co.censo.censo.presentation.biometry_reset

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.components.ActionCompleteUI
import co.censo.shared.presentation.components.DisplayError
import co.censo.censo.R
import co.censo.censo.presentation.biometry_reset.components.BiometryResetApprovalUI
import co.censo.censo.presentation.biometry_reset.components.ReEnrollBiometryUI
import co.censo.censo.presentation.biometry_reset.components.SelectBiometryResetApproverUI
import co.censo.censo.presentation.components.AnotherDeviceInitiatedFlow
import co.censo.censo.presentation.components.AnotherDeviceInitiatedFlowUI
import co.censo.censo.presentation.components.YesNoDialog
import co.censo.censo.presentation.facetec_auth.FacetecAuth
import co.censo.censo.util.external
import co.censo.censo.util.launchSingleTopIfNavigatingToHomeScreen
import co.censo.shared.util.popCurrentDestinationFromBackStack
import co.censo.shared.presentation.components.LargeLoading
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun BiometryResetScreen(
    navController: NavController,
    viewModel: BiometryResetViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> viewModel.onStart()
            Lifecycle.Event.ON_RESUME -> viewModel.onResume()
            Lifecycle.Event.ON_PAUSE -> viewModel.onPause()

            else -> Unit
        }
    }

    LaunchedEffect(key1 = state) {
        if (state.navigationResource is Resource.Success) {
            viewModel.onNavigate()
            state.navigationResource.data.let { navigationData ->
                navController.navigate(navigationData.route) {
                    launchSingleTopIfNavigatingToHomeScreen(navigationData.route)
                    if (navigationData.popSelfFromBackStack) {
                        popCurrentDestinationFromBackStack(navController)
                    }
                }
            }
            viewModel.delayedResetNavigationResource()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    when (state.uiState) {
                        UIState.Completed,
                        UIState.AnotherDevice -> {}

                        else -> {
                            IconButton(onClick = { viewModel.receiveAction(BiometryResetAction.BackClicked) }) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = stringResource(id = R.string.back),
                                )
                            }
                        }
                    }
                },
                title = {
                    Text(
                        text = when (state.uiState) {
                            UIState.Completed -> ""
                            else -> "Biometry reset"
                        },
                        textAlign = TextAlign.Center
                    )
                },
            )
        }) { paddingValues ->

        if (state.showCancelConfirmationDialog) {
            YesNoDialog(
                title = stringResource(R.string.are_you_sure),
                message = stringResource(R.string.cancel_biometry_reset_dialog),
                onDismiss = { viewModel.receiveAction(BiometryResetAction.CancelBiometryResetCancelled) },
                onConfirm = { viewModel.receiveAction(BiometryResetAction.CancelBiometryReset) }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            when {
                state.loading -> {
                    LargeLoading(fullscreen = true)
                }

                state.asyncError -> {
                    when {
                        state.userResponse is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.userResponse.getErrorMessage(context),
                                dismissAction = null,
                                retryAction = {
                                    viewModel.resetUserResponse()
                                    viewModel.receiveAction(BiometryResetAction.Retry)
                                }
                            )
                        }

                        state.initiateBiometryResetResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.initiateBiometryResetResource.getErrorMessage(context),
                                dismissAction = null,
                                retryAction = {
                                    viewModel.resetInitiateBiometryResetResource()
                                    viewModel.receiveAction(BiometryResetAction.Retry)
                                }
                            )
                        }

                        state.cancelBiometryResetResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.cancelBiometryResetResource.getErrorMessage(context),
                                dismissAction = null,
                                retryAction = {
                                    viewModel.resetCancelBiometryResetResource()
                                    viewModel.receiveAction(BiometryResetAction.Retry)
                                }
                            )
                        }

                        state.replaceBiometryResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.replaceBiometryResource.getErrorMessage(context),
                                dismissAction = null,
                                retryAction = {
                                    viewModel.resetReplaceBiometryResource()
                                    viewModel.receiveAction(BiometryResetAction.Retry)
                                }
                            )
                        }
                    }
                }

                else -> {
                    when (state.uiState) {

                        UIState.Initial -> {}

                        UIState.AnotherDevice -> {
                            AnotherDeviceInitiatedFlowUI(
                                flow = AnotherDeviceInitiatedFlow.BiometryReset,
                                onCancel = { viewModel.receiveAction(BiometryResetAction.CancelBiometryReset) }
                            )
                        }

                        UIState.SelectApprover -> {
                            SelectBiometryResetApproverUI(
                                approvals = state.approvals,
                                approvers = state.approvers.external(),
                                selectedApprover = state.selectedApprover,
                                onApproverSelected = { viewModel.receiveAction(BiometryResetAction.ApproverSelectionChanged(it) )},
                                onContinue = { viewModel.receiveAction(BiometryResetAction.ContinueWithSelectedApprover )}
                            )
                        }

                        UIState.ApproveBiometryReset -> {
                            BiometryResetApprovalUI(
                                nickname = state.selectedApprover?.label ?: stringResource(R.string.your_approver_backup_label),
                                secondsLeft = state.secondsLeft,
                                verificationCode = state.approverCodes[state.selectedApprover?.participantId] ?: "",
                                approval = state.approvals.find { it.participantId == state.selectedApprover?.participantId }!!,
                            )
                        }

                        UIState.EnrollNewBiometry -> {
                            ReEnrollBiometryUI(
                                onEnrollBiometry = { viewModel.receiveAction(BiometryResetAction.EnrollNewBiometry) }
                            )
                        }

                        UIState.Facetec -> {
                            FacetecAuth(
                                onFaceScanReady = { verificationId, biometry ->
                                    viewModel.onFaceScanReady(verificationId, biometry)
                                },
                                onCancelled = {
                                    viewModel.receiveAction(BiometryResetAction.FacetecCancelled)
                                }
                            )
                        }

                        UIState.Completed -> {
                            ActionCompleteUI(
                                title = stringResource(id = R.string.you_are_all_set)
                            )

                            LaunchedEffect(Unit) {
                                delay(6000)
                                viewModel.navigateToEntrance()
                            }
                        }
                    }
                }
            }
        }
    }
}