package co.censo.vault.presentation.access_approval

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.components.ActionCompleteUI
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.GetLiveWithUserUI
import co.censo.vault.R
import co.censo.vault.presentation.VaultColors
import co.censo.vault.presentation.access_approval.components.AnotherDeviceAccessScreen
import co.censo.vault.presentation.access_approval.components.ApproveAccessUI
import co.censo.vault.presentation.access_approval.components.SelectApproverUI
import co.censo.vault.presentation.components.YesNoDialog
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AccessApprovalScreen(
    navController: NavController,
    viewModel: AccessApprovalViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                viewModel.onStart()
            }

            Lifecycle.Event.ON_PAUSE -> {
                viewModel.onStop()
            }

            else -> Unit
        }
    }

    LaunchedEffect(key1 = state) {
        if (state.navigationResource is Resource.Success) {
            state.navigationResource.data?.let {
                navController.navigate(it)
            }
            viewModel.resetNavigationResource()
        }

        if (state.initiateNewRecovery) {
            viewModel.initiateRecovery()
        }
    }

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.smallTopAppBarColors(
                containerColor = VaultColors.NavbarColor
            ),
            navigationIcon = {
                when (state.accessApprovalUIState) {
                    AccessApprovalUIState.Approved,
                    AccessApprovalUIState.AnotherDevice -> {}
                    AccessApprovalUIState.GettingLive -> {
                        IconButton(onClick = viewModel::onBackClicked) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = stringResource(id = R.string.exit),
                            )
                        }
                    }
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
                        AccessApprovalUIState.Approved -> ""
                        else -> {
                            stringResource(id = R.string.access)
                        }
                    },
                    textAlign = TextAlign.Center
                )
            },
            actions = {
                IconButton(onClick = {
                    Toast.makeText(context, "Show FAQ Web View", Toast.LENGTH_LONG).show()
                }) {
                    Icon(
                        painterResource(id = co.censo.shared.R.drawable.question),
                        contentDescription = "learn more"
                    )
                }
            })
    }) { paddingValues ->

        if (state.showCancelConfirmationDialog) {
            YesNoDialog(
                title = stringResource(R.string.cancel_access),
                message = stringResource(R.string.cancel_access_dialog),
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
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(72.dp),
                        strokeWidth = 5.dp
                    )
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

                        state.initiateRecoveryResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.initiateRecoveryResource.getErrorMessage(context),
                                dismissAction = null,
                                retryAction = { viewModel.initiateRecovery() }
                            )
                        }

                        state.cancelRecoveryResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.cancelRecoveryResource.getErrorMessage(context),
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

                        AccessApprovalUIState.Initial -> {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(72.dp),
                                strokeWidth = 5.dp
                            )
                        }

                        AccessApprovalUIState.AnotherDevice -> {
                            AnotherDeviceAccessScreen(
                                onCancel = viewModel::cancelAccess
                            )
                        }

                        AccessApprovalUIState.SelectApprover -> {
                            SelectApproverUI(
                                approvers = state.approvers.external(),
                                selectedApprover = state.selectedApprover,
                                onApproverSelected = viewModel::onApproverSelected,
                                onContinue = viewModel::onApproverSelected
                            )
                        }

                        AccessApprovalUIState.GettingLive -> {
                            GetLiveWithUserUI(
                                title = stringResource(R.string.get_live_with_your_approver),
                                message = stringResource(R.string.get_live_with_your_approver_message),
                                onContinueLive = viewModel::onContinueLive,
                                onResumeLater = viewModel::onResumeLater,
                            )
                        }

                        AccessApprovalUIState.ApproveAccess -> {
                            ApproveAccessUI(
                                approval = state.approvals.find { it.participantId == state.selectedApprover?.participantId }!!,
                                verificationCode = state.verificationCode,
                                onVerificationCodeChanged = viewModel::updateVerificationCode,
                                storesLink = "https://censo.co/approvers",
                            )
                        }

                        AccessApprovalUIState.Approved -> {
                            ActionCompleteUI(
                                title = stringResource(id = R.string.approved)
                            )

                            LaunchedEffect(Unit) {
                                delay(3000)
                                viewModel.navigateToViewSeedPhrases()
                            }
                        }
                    }
                }
            }
        }
    }
}