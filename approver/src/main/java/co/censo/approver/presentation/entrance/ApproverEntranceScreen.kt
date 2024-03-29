package co.censo.approver.presentation.entrance

import ParticipantId
import android.annotation.SuppressLint
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.approver.R
import co.censo.approver.presentation.entrance.components.ApproverLanding
import co.censo.approver.presentation.entrance.components.ApproverLoginUI
import co.censo.approver.presentation.entrance.components.LoggedInPasteLinkUI
import co.censo.approver.presentation.entrance.components.LoggedOutPasteLinkUI
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GoogleAuthError
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.presentation.cloud_storage.CloudStorageHandler
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.LargeLoading
import co.censo.shared.util.ClipboardHelper
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.popUpToTop
import co.censo.shared.util.sendError
import com.google.android.gms.auth.api.signin.GoogleSignIn

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ApproverEntranceScreen(
    navController: NavController,
    invitationId: String? = null,
    participantId: String? = null,
    approvalId: String? = null,
    viewModel: ApproverEntranceViewModel = hiltViewModel(),
    appLinkUri: Uri? = null
) {
    val context = LocalContext.current as FragmentActivity

    val state = viewModel.state

    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { activityResult ->
            when (activityResult.resultCode) {
                RESULT_OK -> {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(activityResult.data)
                    viewModel.handleSignInResult(task)
                }

                RESULT_CANCELED -> {
                    viewModel.googleAuthCancel()
                }

                else -> {
                    viewModel.googleAuthFailure(GoogleAuthError.IntentResultFailed)
                }
            }
        }
    )

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart(
            invitationId = invitationId,
            participantId = participantId,
            approvalId = approvalId,
            appLinkUri = appLinkUri
        )
        onDispose { }
    }

    LaunchedEffect(key1 = state) {
        if (state.navigationResource is Resource.Success) {
            state.navigationResource.data.let { navigationData ->
                navController.navigate(navigationData.route) {
                    if (navigationData.popUpToTop) {
                        popUpToTop()
                    }
                }
            }
            viewModel.resetNavigationResource()
        }

        if (state.triggerGoogleSignIn is Resource.Success) {
            try {
                val googleSignInClient = viewModel.getGoogleSignInClient()

                val intent = googleSignInClient.signInIntent
                googleAuthLauncher.launch(intent)
            } catch (e: Exception) {
                e.sendError(CrashReportingUtil.SignIn)
                viewModel.googleAuthFailure(GoogleAuthError.FailedToLaunchGoogleAuthUI(e))
            }
            viewModel.resetTriggerGoogleSignIn()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(color = Color.White),
    ) {
        when {
            state.isLoading -> {
                LargeLoading(
                    color = SharedColors.DefaultLoadingColor,
                    fullscreen = true,
                    fullscreenBackgroundColor = Color.White
                )
            }

            state.apiCallErrorOccurred -> {
                if (state.signInUserResource is Resource.Error) {
                    DisplayError(
                        errorMessage = state.signInUserResource.getErrorMessage(context),
                        dismissAction = viewModel::resetSignInUserResource,
                    ) { viewModel.retrySignIn() }
                } else if (state.triggerGoogleSignIn is Resource.Error) {
                    DisplayError(
                        errorMessage = state.triggerGoogleSignIn.getErrorMessage(context),
                        dismissAction = viewModel::resetTriggerGoogleSignIn,
                    ) { viewModel.retrySignIn() }
                }
            }

            state.linkError -> {
                DisplayError(
                    errorMessage = "${stringResource(R.string.link_not_valid)} - (${
                        ClipboardHelper.getClipboardContent(
                            context
                        )
                    })",
                    dismissAction = { viewModel.clearError() },
                    retryAction = null
                )
            }

            else -> {
                when (val uiState = state.uiState) {
                    ApproverEntranceUIState.Initial -> {
                        LargeLoading(
                            color = SharedColors.DefaultLoadingColor,
                            fullscreen = true,
                            fullscreenBackgroundColor = Color.White
                        )
                    }

                    ApproverEntranceUIState.LoggedOutPasteLink -> {
                        LoggedOutPasteLinkUI(
                            onPasteLinkClick = {
                                viewModel.handleLoggedOutLink(
                                    ClipboardHelper.getClipboardContent(context)
                                )
                            }
                        )
                    }

                    is ApproverEntranceUIState.LoggedInPasteLink -> {
                        LoggedInPasteLinkUI(
                            isApprover = uiState.isActiveApprover,
                            onPasteLinkClick = {
                                viewModel.handleLoggedInLink(
                                    ClipboardHelper.getClipboardContent(context)
                                )
                            },
                            onAssistClick = viewModel::navToResetLinks,
                            onBackClick = viewModel::backToLanding
                        )
                    }

                    ApproverEntranceUIState.SignIn -> {
                        ApproverLoginUI(
                            authenticate = { viewModel.startGoogleSignInFlow() }
                        )
                    }

                    is ApproverEntranceUIState.Landing -> {
                        ApproverLanding(
                            loggedIn = state.loggedIn,
                            isActiveApprover = uiState.isActiveApprover,
                            onSettingsClick = viewModel::navToSettingsScreen,
                            onContinue = viewModel::onLandingContinue
                        )
                    }
                }
            }
        }

        if (state.forceUserToGrantCloudStorageAccess.requestAccess) {
            CloudStorageHandler(
                actionToPerform = CloudStorageActions.ENFORCE_ACCESS,
                participantId = ParticipantId(""),
                encryptedPrivateKey = null,
                onActionSuccess = {},
                onActionFailed = {},
                onCloudStorageAccessGranted = { viewModel.handleCloudStorageAccessGranted() }
            )
        }
    }
}