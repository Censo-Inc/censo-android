package co.censo.approver.presentation.onboarding

import ParticipantId
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
import co.censo.approver.presentation.components.ApproverCodeVerification
import co.censo.approver.presentation.components.ApproverTopBar
import co.censo.approver.presentation.components.CodeVerificationStatus
import co.censo.approver.presentation.components.PostApproverAction
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.presentation.cloud_storage.CloudStorageHandler
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.LargeLoading

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
                    viewModel.triggerApproverRoutingNavigation()
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
        if (state.navToApproverRouting) {
            navController.navigate(Screen.ApproverEntranceRoute.route) {
                popUpTo(Screen.ApproverOnboardingScreen.route) {
                    inclusive = true
                }
            }

            viewModel.resetApproverRoutingNavigationTrigger()
        }

        if (state.onboardingMessage is Resource.Success) {
            val message = when (state.onboardingMessage.data) {
                OnboardingMessage.FailedPasteLink -> context.getString(R.string.failed_to_get_invite_clipboard)
                OnboardingMessage.LinkPastedSuccessfully -> context.getString(R.string.found_invite_clipboard)
                OnboardingMessage.LinkAccepted -> context.getString(R.string.accepted_as_an_approver)
                OnboardingMessage.CodeAccepted -> context.getString(R.string.owner_accepted_code)
                null -> null
            }

            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
            viewModel.resetMessage()
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
                                    color = Color.Black,
                                    fullscreen = true
                                )
                            }

                            ApproverOnboardingUIState.NeedsToSaveKey -> {
                                NeedsToSaveKey(viewModel::createKeyAndTriggerCloudSave)
                            }
                            ApproverOnboardingUIState.NeedsToEnterCode,
                            ApproverOnboardingUIState.CodeRejected,
                            ApproverOnboardingUIState.WaitingForConfirmation -> {
                                ApproverCodeVerification(
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
                                PostApproverAction()
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

    if (state.cloudStorageAction.triggerAction) {
        val privateKey =
            if (state.cloudStorageAction.action == CloudStorageActions.UPLOAD) {
                viewModel.getEncryptedKeyForUpload()
            } else null

        CloudStorageHandler(
            actionToPerform = state.cloudStorageAction.action,
            participantId = ParticipantId(state.participantId),
            encryptedPrivateKey = privateKey,
            onActionSuccess = { byteArray ->
                viewModel.handleCloudStorageActionSuccess(
                    byteArray,
                    state.cloudStorageAction.action
                )
            },
            onActionFailed = { exception ->
                viewModel.handleCloudStorageActionFailure(
                    exception,
                    state.cloudStorageAction.action
                )
            },
        )
    }
}

@Composable
fun NeedsToSaveKey(
    onSaveKey: () -> Unit
) {
    Text(
        modifier = Modifier.padding(horizontal = 12.dp),
        text = stringResource(R.string.key_failed_to_save_please_save_again),
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = onSaveKey) {
        Text(text = stringResource(R.string.save_key_to_cloud))
    }
}
