package co.censo.vault.presentation.recovery

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.model.Recovery
import co.censo.shared.data.model.RecoveryStatus
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.components.DisplayError
import co.censo.vault.presentation.VaultColors
import co.censo.vault.presentation.components.recovery.AccessPhrasesScreen
import co.censo.vault.presentation.components.recovery.AnotherDeviceRecoveryScreen
import co.censo.vault.presentation.components.recovery.RecoveryApprovalCodeVerificationModal
import co.censo.vault.presentation.components.recovery.ThisDeviceRecoveryScreen

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun RecoveryScreen(
    navController: NavController,
    viewModel: RecoveryScreenViewModel = hiltViewModel()
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
            viewModel.reset()
        }

        if (state.initiateNewRecovery) {
            viewModel.initiateRecovery()
        }
    }

    when {
        state.loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = VaultColors.PrimaryColor)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(72.dp)
                        .align(Alignment.Center),
                    strokeWidth = 8.dp,
                    color = Color.White
                )
            }
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
                        errorMessage = state.initiateRecoveryResource.getErrorMessage(context),
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
                        errorMessage = state.submitTotpVerificationResource.getErrorMessage(context),
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

                    when (recovery.status) {
                        RecoveryStatus.Requested -> {
                            ThisDeviceRecoveryScreen(
                                recovery,
                                guardians = state.guardians,
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
            }
        }
    }
}
