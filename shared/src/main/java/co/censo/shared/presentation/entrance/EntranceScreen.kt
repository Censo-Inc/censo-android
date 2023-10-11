package co.censo.shared.presentation.entrance

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import co.censo.shared.BuildConfig
import co.censo.shared.data.Resource
import co.censo.shared.R
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.util.projectLog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntranceScreen(
    navController: NavController,
    guardianEntrance: Boolean,
    invitationId: String? = null,
    participantId: String? = null,
    viewModel: EntranceViewModel = hiltViewModel()
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
            projectLog(message = "checkPermissionDialog exception caught: ${e.message}")
            //TODO: raygun
        }
    }

    fun signOutFromGoogle() {
        try {
            val gso = GoogleSignInOptions.Builder()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(context, gso)

            googleSignInClient.signOut()
        } catch (e: Exception) {
            viewModel.googleAuthFailure(GoogleAuthError.FailedToSignUserOut(e))
        }
        viewModel.signUserOut()

    }

    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { activityResult ->
            when (activityResult.resultCode) {
                RESULT_OK -> {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(activityResult.data)
                    viewModel.handleSignInResult(task)
                }

                RESULT_CANCELED -> viewModel.googleAuthFailure(GoogleAuthError.UserCanceledOneTap)
                else -> viewModel.googleAuthFailure(GoogleAuthError.IntentResultFailed)
            }
        }
    )

    DisposableEffect(key1 = viewModel) {
        if (guardianEntrance) {
            viewModel.onGuardianStart(invitationId, participantId)
        } else {
            viewModel.onOwnerStart()
        }
        onDispose { }
    }

    LaunchedEffect(key1 = state) {
        if (state.userFinishedSetup is Resource.Success) {
            state.userFinishedSetup.data?.let {
                navController.navigate(it)
            }
            viewModel.resetUserFinishedSetup()
        }

        if (state.triggerGoogleSignIn is Resource.Success) {
                try {
                    val gso = GoogleSignInOptions.Builder()
                        .requestIdToken(BuildConfig.GOOGLE_AUTH_SERVER_ID)
                        .build()

                    val googleSignInClient = GoogleSignIn.getClient(context, gso)

                    val intent = googleSignInClient.signInIntent
                    googleAuthLauncher.launch(intent)
                } catch (e: Exception) {
                    viewModel.googleAuthFailure(GoogleAuthError.FailedToLaunchGoogleAuthUI(e))
                }

            viewModel.resetTriggerGoogleSignIn()
        }

        if (state.showPushNotificationsDialog is Resource.Success) {
            checkNotificationsPermissionDialog()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier
                    .fillMaxWidth(),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.entrance_screen))
                    }
                })
        },
        content = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(color = Color.White),
            ) {

                when {
                    state.isLoading -> {
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
                                color = Color.Red
                            )
                        }
                    }

                    state.apiCallErrorOccurred -> {
                        if (state.createUserResource is Resource.Error) {
                            DisplayError(
                                errorMessage = state.createUserResource.getErrorMessage(context),
                                dismissAction = viewModel::resetCreateOwnerResource,
                            ) { viewModel.retryCreateUser() }
                        } else if (state.triggerGoogleSignIn is Resource.Error) {
                            DisplayError(
                                errorMessage = state.triggerGoogleSignIn.getErrorMessage(context),
                                dismissAction = viewModel::resetCreateOwnerResource,
                            ) { viewModel.retryCreateUser() }
                        }
                    }

                    else -> {
                        OwnerEntranceStandardUI(
                            authenticate = { viewModel.startGoogleSignInFlow() },
                            signOut = { signOutFromGoogle() }
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun OwnerEntranceStandardUI(
    authenticate: () -> Unit,
    signOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(onClick = authenticate) {
            Text(text = stringResource(R.string.google_auth_login))
        }

        TextButton(onClick = signOut) {
            Text(text = stringResource(R.string.sign_out))
        }
    }
}

sealed class GoogleAuthError(val exception: Exception) {
    object InvalidToken : GoogleAuthError(Exception("Invalid Token"))
    object MissingCredentialId : GoogleAuthError(Exception("Missing Google Credential Id"))
    object UserCanceledOneTap : GoogleAuthError(Exception("User Canceled Google Auth"))
    object IntentResultFailed : GoogleAuthError(Exception("Intent Result Failed"))
    data class ErrorParsingIntent(val e: Exception) : GoogleAuthError(e)
    data class FailedToSignUserOut(val e: Exception) : GoogleAuthError(e)
    data class FailedToLaunchGoogleAuthUI(val e: Exception) : GoogleAuthError(e)
    data class FailedToVerifyId(val e: Exception) : GoogleAuthError(e)
}