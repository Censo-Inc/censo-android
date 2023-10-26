package co.censo.vault.presentation.access_approval

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import co.censo.shared.data.model.Recovery
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.components.DisplayError
import co.censo.vault.R
import co.censo.vault.presentation.VaultColors
import co.censo.vault.presentation.access_approval.components.ApproveAccessUI
import co.censo.vault.presentation.access_approval.components.SelectApproverUI
import co.censo.vault.presentation.components.recovery.AnotherDeviceRecoveryScreen
import co.censo.vault.presentation.plan_setup.components.GetLiveWithApproverUI

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
            Lifecycle.Event.ON_START -> {
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
        TopAppBar(
            colors = TopAppBarDefaults.smallTopAppBarColors(
                containerColor = VaultColors.NavbarColor
            ),
            navigationIcon = {
                IconButton(onClick = viewModel::onBackClicked) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.back),
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(id = R.string.access),
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
                            ) { viewModel.reloadOwnerState() }
                        }

                        state.initiateRecoveryResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.initiateRecoveryResource.getErrorMessage(
                                    context
                                ),
                                dismissAction = null,
                            ) { viewModel.initiateRecovery() }
                        }

                        state.cancelRecoveryResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.cancelRecoveryResource.getErrorMessage(context),
                                dismissAction = null,
                            ) { viewModel.cancelRecovery() }
                        }

                        state.submitTotpVerificationResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.submitTotpVerificationResource.getErrorMessage(
                                    context
                                ),
                                dismissAction = viewModel::dismissVerification,
                                retryAction = null
                            )
                        }
                    }
                }

                else -> {

                    when (val recovery = state.recovery) {
                        null -> {
                            // recovery is about to be requested
                        }

                        is Recovery.AnotherDevice -> {
                            AnotherDeviceRecoveryScreen()
                        }

                        is Recovery.ThisDevice -> {

                            when (state.accessApprovalUIState) {
                                AccessApprovalUIState.GettingLive -> {
                                    GetLiveWithApproverUI(
                                        nickname = "your approver",
                                        onContinueLive = viewModel::onContinueLive,
                                        onResumeLater = viewModel::onResumeLater,
                                    )
                                }

                                AccessApprovalUIState.SelectApprover -> {
                                    SelectApproverUI(
                                        approvers = state.approvers.external(),
                                        selectedApprover = state.selectedApprover,
                                        onApproverSelected = viewModel::onApproverSelected,
                                        onContinue = viewModel::continueToApproveAccess
                                    )
                                }

                                AccessApprovalUIState.ApproveAccess -> {
                                    ApproveAccessUI(
                                        approval = state.approvals.find { it.participantId == state.selectedApprover?.participantId }!!,
                                        verificationCode = state.verificationCode,
                                        onVerificationCodeChanged = viewModel::onVerificationCodeChanged,
                                        storesLink = "",
                                        onContinue = {}
                                    )
                                }

                                AccessApprovalUIState.Approved -> {
                                }
                            }
                        }

                        /*when (val recovery = state.recovery) {
                        null -> {
                            // recovery is about to be requested
                        }

                        is Recovery.AnotherDevice -> {
                            AnotherDeviceRecoveryScreen()
                        }

                        is Recovery.ThisDevice -> {

                            when (recovery.status) {
                                RecoveryStatus.Requested -> {
                                    ThisDeviceRecoveryScreen(
                                        recovery,
                                        guardians = state.approvers,
                                        approvalsRequired = state.approvalsRequired,
                                        approvalsCollected = state.approvalsCollected,
                                        onCancelRecovery = viewModel::cancelRecovery,
                                        onEnterCode = { approval ->
                                            viewModel.showCodeEntryModal(approval)
                                        }
                                    )

                                    if (state.totpVerificationState.showModal) {
                                        RecoveryApprovalCodeVerificationModal(
                                            approverLabel = state.totpVerificationState.approverLabel,
                                            validCodeLength = TotpGenerator.CODE_LENGTH,
                                            value = state.totpVerificationState.verificationCode,
                                            onValueChanged = { code: String ->
                                                viewModel.updateVerificationCode(
                                                    state.totpVerificationState.participantId,
                                                    code
                                                )
                                            },
                                            onDismiss = viewModel::dismissVerification,
                                            isLoading = state.submitTotpVerificationResource is Resource.Loading,
                                            isWaitingForVerification = state.totpVerificationState.waitingForApproval,
                                            isVerificationRejected = state.totpVerificationState.rejected
                                        )
                                    }
                                }

                                else -> {
                                    AccessPhrasesScreen(
                                        recovery,
                                        approvalsRequired = state.approvalsRequired,
                                        approvalsCollected = state.approvalsCollected,
                                        onCancelRecovery = viewModel::cancelRecovery,
                                        onRecoverPhrases = viewModel::onRecoverPhrases
                                    )
                                }
                            }
                        }
                    }*/
                    }
                }
            }
        }
    }
}