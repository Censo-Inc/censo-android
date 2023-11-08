package co.censo.approver.presentation.entrance

import ParticipantId
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.approver.R
import co.censo.approver.data.ApproverEntranceUIState
import co.censo.approver.presentation.Screen
import co.censo.approver.presentation.entrance.components.ApproverLoginUI
import co.censo.approver.presentation.entrance.components.LoggedInPasteLinkUI
import co.censo.approver.presentation.entrance.components.LoggedOutPasteLinkUI
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GoogleAuthError
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.presentation.cloud_storage.CloudStorageHandler
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.util.ClipboardHelper
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.sendError
import com.google.android.gms.auth.api.signin.GoogleSignIn

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ApproverEntranceScreen(
    navController: NavController,
    invitationId: String? = null,
    recoveryParticipantId: String? = null,
    viewModel: ApproverEntranceViewModel = hiltViewModel()
) {
    val context = LocalContext.current as FragmentActivity

    val state = viewModel.state

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {
            viewModel.finishPushNotificationDialog()
        }
    )

    fun checkNotificationsPermissionDialog() {
        try {
            val notificationGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                )

            if (notificationGranted != PackageManager.PERMISSION_GRANTED) {
                val shownPermissionBefore =
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                val seenDialogBefore = viewModel.userHasSeenPushDialog()

                if (!shownPermissionBefore && !seenDialogBefore) {
                    viewModel.setUserSeenPushDialog(true)
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

        } catch (e: Exception) {
            e.sendError(CrashReportingUtil.PermissionDialog)
        }
    }

    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { activityResult ->
            when (activityResult.resultCode) {
                RESULT_OK -> {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(activityResult.data)
                    viewModel.handleSignInResult(task)
                }

                RESULT_CANCELED -> {
                    Exception("User canceled google auth").sendError(CrashReportingUtil.SignIn)
                    viewModel.googleAuthFailure(GoogleAuthError.UserCanceledGoogleSignIn)
                }
                else -> {
                    Exception("Google auth intent result failed").sendError(CrashReportingUtil.SignIn)
                    viewModel.googleAuthFailure(GoogleAuthError.IntentResultFailed)
                }
            }
        }
    )

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart(invitationId, recoveryParticipantId)
        onDispose { }
    }

    LaunchedEffect(key1 = state) {
        if (state.navigationResource is Resource.Success) {
            state.navigationResource.data?.let { destination ->
                navController.navigate(destination) {
                    popUpTo(Screen.ApproverEntranceRoute.route) {
                        inclusive = true
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

        if (state.showPushNotificationsDialog) {
            checkNotificationsPermissionDialog()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(color = Color.White),
    ) {
        when {
            state.isLoading -> {
                Loading()
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
                    errorMessage = "${stringResource(R.string.link_not_valid)} - (${ClipboardHelper.getClipboardContent(context)})",
                    dismissAction = { viewModel.clearError() },
                    retryAction = null
                )
            }

            else -> {
                when (val uiState = state.uiState) {
                    ApproverEntranceUIState.Initial -> {
                        Loading()
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
                            }
                        )
                    }

                    ApproverEntranceUIState.SignIn -> {
                        ApproverLoginUI(
                            authenticate = { viewModel.startGoogleSignInFlow() }
                        )
                    }
                }

                if (state.forceUserToGrantCloudStorageAccess.requestAccess) {
                    CloudStorageHandler(
                        actionToPerform = CloudStorageActions.ENFORCE_ACCESS,
                        participantId = ParticipantId(""),
                        privateKey = null,
                        onActionSuccess = {},
                        onActionFailed = {},
                        onCloudStorageAccessGranted = { viewModel.handleCloudStorageAccessGranted() }
                    )
                }
            }
        }
    }
}

@Composable
private fun Loading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
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
