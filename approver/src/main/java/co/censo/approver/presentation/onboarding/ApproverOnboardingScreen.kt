package co.censo.approver.presentation.onboarding

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import co.censo.approver.presentation.components.ApproverTopBar
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.presentation.components.CodeVerificationStatus
import co.censo.shared.presentation.components.CodeVerificationUI
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.LargeLoading
import co.censo.shared.presentation.components.NeedsToSaveKeyUI
import co.censo.shared.presentation.components.PostSuccessAction
import co.censo.shared.util.popCurrentDestinationFromBackStack
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproverOnboardingScreen(
    navController: NavController,
    viewModel: ApproverOnboardingViewModel = hiltViewModel(),
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> viewModel.onStart()

            Lifecycle.Event.ON_PAUSE -> {
                if (state.approverUIState is ApproverOnboardingUIState.Complete) {
                    viewModel.triggerApproverEntranceNavigation()
                } else {
                    viewModel.onStop()
                }
            }

            else -> Unit
        }
    }

    DisposableEffect(key1 = viewModel) {
        onDispose { viewModel.onDispose() }
    }

    LaunchedEffect(key1 = state) {
        if (state.navToApproverEntrance) {
            navController.navigate(Screen.ApproverEntranceRoute.route) {
                popCurrentDestinationFromBackStack(navController)
            }

            viewModel.resetApproverEntranceNavigationTrigger()
        }

        if (state.onboardingMessage is Resource.Success) {
            val message = when (state.onboardingMessage.data) {
                OnboardingMessage.FailedPasteLink -> context.getString(R.string.failed_to_get_invite_clipboard)
                OnboardingMessage.LinkPastedSuccessfully -> context.getString(R.string.found_invite_clipboard)
                OnboardingMessage.LinkAccepted -> context.getString(R.string.accepted_as_an_approver)
                OnboardingMessage.CodeAccepted -> context.getString(R.string.owner_accepted_code)
            }

            message.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
            viewModel.resetMessage()
        }
    }

    LaunchedEffect(key1 = state) {
        if (state.navigationResource is Resource.Success) {
            state.navigationResource.data.let { navigationData ->
                navController.navigate(navigationData.route)
            }
            viewModel.resetNavigationResource()
        }
    }

    Scaffold(
        topBar = {
            if (state.showTopBar) {
                ApproverTopBar { viewModel.showCloseConfirmationDialog() }
            }
        },
        content = { contentPadding ->

            when {
                state.asyncError -> {
                    when {
                        state.userResponse is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.userResponse.getErrorMessage(context),
                                dismissAction = null,
                            ) { viewModel.retrieveApproverState(false) }
                        }

                        state.acceptApproverResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.acceptApproverResource.getErrorMessage(
                                    context
                                ),
                                dismissAction = { viewModel.resetAcceptApproverResource() },
                            ) { viewModel.resetAcceptApproverResource() }
                        }

                        state.submitVerificationResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.submitVerificationResource.getErrorMessage(
                                    context
                                ),
                                dismissAction = { viewModel.resetSubmitVerificationResource() },
                            ) { viewModel.submitVerificationCode() }
                        }

                        state.savePrivateKeyToCloudResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.savePrivateKeyToCloudResource.getErrorMessage(
                                    context
                                ),
                                dismissAction = { viewModel.createKeyAndTriggerCloudSave() }
                            ) { viewModel.createKeyAndTriggerCloudSave() }
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

                        when (state.approverUIState) {
                            ApproverOnboardingUIState.Loading -> {
                                LargeLoading(
                                    color = SharedColors.DefaultLoadingColor,
                                    fullscreen = true
                                )
                            }

                            ApproverOnboardingUIState.NeedsToSaveKey -> {
                                NeedsToSaveKeyUI(
                                    message = stringResource(R.string.key_failed_to_save_please_save_again),
                                    onSaveKey = viewModel::createKeyAndTriggerCloudSave
                                )
                            }
                            ApproverOnboardingUIState.NeedsToEnterCode,
                            ApproverOnboardingUIState.CodeRejected,
                            ApproverOnboardingUIState.WaitingForConfirmation -> {
                                CodeVerificationUI(
                                    value = state.verificationCode,
                                    onValueChanged = viewModel::updateVerificationCode,
                                    validCodeLength = TotpGenerator.CODE_LENGTH,
                                    isLoading = state.submitVerificationResource is Resource.Loading,
                                    codeVerificationStatus = when (state.approverUIState) {
                                        ApproverOnboardingUIState.WaitingForConfirmation -> CodeVerificationStatus.Waiting
                                        ApproverOnboardingUIState.CodeRejected -> CodeVerificationStatus.Rejected
                                        else -> CodeVerificationStatus.Initial
                                    }
                                )
                            }

                            ApproverOnboardingUIState.Complete -> {
                                PostSuccessAction()
                                LaunchedEffect(state.approverUIState) {
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
}
