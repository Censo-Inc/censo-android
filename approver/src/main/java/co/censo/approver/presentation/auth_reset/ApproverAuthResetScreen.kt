package co.censo.approver.presentation.auth_reset

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
import co.censo.approver.presentation.ApproverColors
import co.censo.approver.presentation.Screen
import co.censo.approver.presentation.components.ApproveRequest
import co.censo.approver.presentation.components.ApproverTopBar
import co.censo.approver.presentation.components.PostApproverAction
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.cloud_storage.CloudAccessEnforcer
import co.censo.shared.presentation.components.CodeVerificationStatus
import co.censo.shared.presentation.components.CodeVerificationUI
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.LargeLoading
import co.censo.shared.util.popCurrentDestinationFromBackStack
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproverAuthResetScreen(
    navController: NavController,
    viewModel: ApproverAuthResetViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> viewModel.onStart()
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
                                retryAction = { viewModel.retrieveApproverState() },
                            )
                        }

                        state.acceptAuthResetResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.acceptAuthResetResource.getErrorMessage(context),
                                dismissAction = viewModel::resetAcceptAuthResetResource,
                                retryAction = viewModel::acceptAuthResetRequest
                            )
                        }

                        state.submitAuthResetVerificationResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.submitAuthResetVerificationResource.getErrorMessage(context),
                                dismissAction = viewModel::resetSubmitAuthResetVerificationResource,
                                retryAction = {
                                    viewModel.resetSubmitAuthResetVerificationResource()
                                    viewModel.checkAuthResetConfirmationPhase()
                                },
                            )
                        }

                        state.authResetNotInProgress is Resource.Error -> {
                            DisplayError(
                                errorMessage = context.getString(R.string.auth_reset_not_in_progress),
                                dismissAction = viewModel::resetAuthResetNotInProgressResource,
                                retryAction = null
                            )
                        }

                        else -> {
                            DisplayError(
                                errorMessage = stringResource(R.string.something_went_wrong),
                                dismissAction = null,
                                retryAction = { viewModel.retrieveApproverState() },
                            )
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

                        when (state.uiState) {
                            ApproverAuthResetState.UIState.AuthenticationResetRequested -> {
                                ApproveRequest(onContinue = viewModel::acceptAuthResetRequest)
                            }

                            ApproverAuthResetState.UIState.NeedsToEnterCode,
                            ApproverAuthResetState.UIState.WaitingForVerification,
                            ApproverAuthResetState.UIState.CodeRejected, -> {
                                CodeVerificationUI(
                                    value = state.verificationCode,
                                    onValueChanged = viewModel::updateVerificationCode,
                                    validCodeLength = TotpGenerator.CODE_LENGTH,
                                    isLoading = state.submitAuthResetVerificationResource is Resource.Loading,
                                    codeVerificationStatus = when (state.uiState) {
                                        ApproverAuthResetState.UIState.WaitingForVerification -> CodeVerificationStatus.Waiting
                                        ApproverAuthResetState.UIState.CodeRejected -> CodeVerificationStatus.Rejected
                                        else -> CodeVerificationStatus.Initial
                                    }
                                )
                            }

                            ApproverAuthResetState.UIState.Complete -> {
                                PostApproverAction()
                                LaunchedEffect(state.uiState) {
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