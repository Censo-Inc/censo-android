package co.censo.censo.presentation.access_approval

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
import co.censo.censo.presentation.access_approval.components.ApproveAccessUI
import co.censo.censo.presentation.access_approval.components.SelectApprover
import co.censo.censo.presentation.components.AnotherDeviceInitiatedFlow
import co.censo.censo.presentation.components.AnotherDeviceInitiatedFlowUI
import co.censo.censo.presentation.components.YesNoDialog
import co.censo.censo.util.external
import co.censo.censo.util.launchSingleTopIfNavigatingToHomeScreen
import co.censo.shared.util.popCurrentDestinationFromBackStack
import co.censo.shared.data.model.AccessIntent
import co.censo.shared.presentation.components.LargeLoading
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AccessApprovalScreen(
    navController: NavController,
    accessIntent: AccessIntent,
    viewModel: AccessApprovalViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> viewModel.onStart(accessIntent)
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
                    when (state.accessApprovalUIState) {
                        AccessApprovalUIState.Initial,
                        AccessApprovalUIState.Approved,
                        AccessApprovalUIState.AnotherDevice -> {}

                        else -> {
                            IconButton(onClick = viewModel::onBackClicked) {
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
                        text = when (state.accessApprovalUIState) {
                            AccessApprovalUIState.Initial,
                            AccessApprovalUIState.Approved -> ""

                            else -> when (accessIntent) {
                                AccessIntent.AccessPhrases -> stringResource(id = R.string.access)
                                AccessIntent.ReplacePolicy -> stringResource(id = R.string.remove_approvers)
                                AccessIntent.RecoverOwnerKey -> stringResource(id = R.string.recover_key)
                            }
                        },
                        textAlign = TextAlign.Center
                    )
                },
            )
        }) { paddingValues ->

        if (state.showCancelConfirmationDialog) {
            YesNoDialog(
                title = stringResource(if (state.accessIntent == AccessIntent.RecoverOwnerKey) R.string.cancel_key_recovery else R.string.cancel_access),
                message = stringResource(if (state.accessIntent == AccessIntent.RecoverOwnerKey) R.string.cancel_key_recovery_dialog else R.string.cancel_access_dialog),
                onDismiss = viewModel::hideCloseConfirmationDialog,
                onConfirm = viewModel::cancelAccess
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
                                retryAction = { viewModel.retrieveOwnerState() }
                            )
                        }

                        state.initiateAccessResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.initiateAccessResource.getErrorMessage(context),
                                dismissAction = null,
                                retryAction = { viewModel.initiateAccess() }
                            )
                        }

                        state.cancelAccessResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.cancelAccessResource.getErrorMessage(context),
                                dismissAction = null,
                                retryAction = { viewModel.cancelAccess() }
                            )
                        }

                        state.submitTotpVerificationResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.submitTotpVerificationResource.getErrorMessage(context),
                                dismissAction = null,
                                retryAction = { viewModel.updateVerificationCode(state.verificationCode) }
                            )
                        }
                    }
                }

                else -> {
                    when (state.accessApprovalUIState) {

                        AccessApprovalUIState.Initial -> {}

                        AccessApprovalUIState.AnotherDevice -> {
                            AnotherDeviceInitiatedFlowUI(
                                flow = AnotherDeviceInitiatedFlow.Access,
                                onCancel = viewModel::cancelAccess
                            )
                        }

                        AccessApprovalUIState.SelectApprover -> {
                            SelectApprover(
                                intent = accessIntent,
                                approvals = state.approvals,
                                approvers = state.approvers.external(),
                                selectedApprover = state.selectedApprover,
                                onApproverSelected = viewModel::onApproverSelected,
                                onContinue = viewModel::onContinue
                            )
                        }

                        AccessApprovalUIState.ApproveAccess -> {
                            ApproveAccessUI(
                                intent = accessIntent,
                                approverName = state.selectedApprover?.label ?: stringResource(R.string.your_approver_backup_label),
                                approval = state.approvals.find { it.participantId == state.selectedApprover?.participantId }!!,
                                verificationCode = state.verificationCode,
                                onVerificationCodeChanged = viewModel::updateVerificationCode,
                            )
                        }

                        AccessApprovalUIState.Approved -> {
                            ActionCompleteUI(
                                title = stringResource(id = R.string.approved)
                            )

                            LaunchedEffect(Unit) {
                                delay(3000)
                                if (viewModel.state.isTimelocked) {
                                    viewModel.setNavigateBackToHome()
                                } else {
                                    viewModel.navigateIntentAware()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}