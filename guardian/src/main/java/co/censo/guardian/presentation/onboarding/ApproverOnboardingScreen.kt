package co.censo.guardian.presentation.onboarding

import ParticipantId
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import co.censo.guardian.R
import co.censo.guardian.data.ApproverOnboardingUIState
import co.censo.guardian.presentation.GuardianColors
import co.censo.guardian.presentation.Screen
import co.censo.guardian.presentation.components.ApproverCodeVerification
import co.censo.guardian.presentation.components.ApproverTopBar
import co.censo.guardian.presentation.components.CodeVerificationStatus
import co.censo.guardian.presentation.components.PasteLinkHomeScreen
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.presentation.cloud_storage.CloudStorageHandler
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.util.ClipboardHelper

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

    DisposableEffect(key1 = viewModel) {
        onDispose { viewModel.onDispose() }
    }

    LaunchedEffect(key1 = state) {
        if (state.navToApproverAccess) {
            navController.navigate(Screen.ApproverAccessScreen.route) {
                popUpTo(Screen.ApproverOnboardingScreen.route) {
                    inclusive = true
                }
            }

            viewModel.resetApproverAccessNavigationTrigger()
        }

        if (state.onboardingMessage is Resource.Success) {
            val message = when (state.onboardingMessage.data) {
                OnboardingMessage.FailedPasteLink -> "Failed to get invite from clipboard. Please try again."
                OnboardingMessage.LinkPastedSuccessfully -> "Found invite from clipboard!"
                OnboardingMessage.LinkAccepted -> "Accepted as an approver. Let's get you verified."
                OnboardingMessage.CodeAccepted -> "Owner has accepted your code!"
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
            ApproverTopBar(
                uiState = state.approverUIState,
                onClose = viewModel::showCloseConfirmationDialog
            )
        },
        content = { contentPadding ->

            when {
                state.loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(72.dp)
                                .align(Alignment.Center),
                            strokeWidth = 8.dp,
                            color = Color.Black
                        )
                    }
                }

                state.asyncError -> {
                    when {
                        state.userResponse is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.userResponse.getErrorMessage(context),
                                dismissAction = null,
                            ) { viewModel.retrieveApproverState(false) }
                        }

                        state.acceptGuardianResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.acceptGuardianResource.getErrorMessage(
                                    context
                                ),
                                dismissAction = { viewModel.resetAcceptGuardianResource() },
                            ) { viewModel.retrieveApproverState(false) }
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

                            ApproverOnboardingUIState.UserNeedsPasteLink -> {
                                PasteLinkHomeScreen {
                                    viewModel.userPastedInviteCode(
                                        ClipboardHelper.getClipboardContent(context)
                                    )
                                }
                            }

                            else -> {}
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
                    color = GuardianColors.PrimaryColor,
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
                viewModel.getPrivateKeyForUpload()
            } else null

        if (state.loadingCloudAction) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(72.dp)
                        .align(Alignment.Center),
                    strokeWidth = 8.dp,
                    color = Color.Black
                )
            }
        }

        CloudStorageHandler(
            actionToPerform = state.cloudStorageAction.action,
            participantId = ParticipantId(state.participantId),
            privateKey = privateKey,
            onActionSuccess = { base58EncodedPrivateKey ->
                viewModel.handleCloudStorageActionSuccess(
                    base58EncodedPrivateKey,
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
