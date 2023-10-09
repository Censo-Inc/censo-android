package co.censo.shared.presentation.entrance

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
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
import co.censo.shared.data.Resource
import co.censo.shared.BuildConfig
import co.censo.shared.R
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.util.projectLog
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntranceScreen(
    navController: NavController,
    guardianEntrance: Boolean,
    invitationId: String = "",
    viewModel: EntranceViewModel = hiltViewModel()
) {

    val context = LocalContext.current as FragmentActivity

    val oneTapClient: SignInClient = Identity.getSignInClient(context)
    lateinit var signInRequest: BeginSignInRequest

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

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { activityResult ->
            when (activityResult.resultCode) {
                RESULT_OK -> {
                    try {
                        val credential =
                            oneTapClient.getSignInCredentialFromIntent(activityResult.data)
                        credential.googleIdToken?.let { viewModel.oneTapSuccess(it) }
                            ?: viewModel.oneTapFailure(OneTapError.MissingCredentialId)
                    } catch (e: Exception) {
                        viewModel.oneTapFailure(OneTapError.ErrorParsingIntent(e))
                    }
                }

                RESULT_CANCELED -> viewModel.oneTapFailure(OneTapError.UserCanceledOneTap)
                else -> viewModel.oneTapFailure(OneTapError.IntentResultFailed)
            }
        }
    )

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart(invitationId = invitationId, guardianEntrance = guardianEntrance)
        onDispose { }
    }

    LaunchedEffect(key1 = state) {
        if (state.userFinishedSetup is Resource.Success) {
            state.userFinishedSetup.data?.let {
                navController.navigate(it)
            }
            viewModel.resetUserFinishedSetup()
        }

        if (state.triggerOneTap is Resource.Success) {
            signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                    BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId(BuildConfig.ONE_TAP_SERVER_ID)
                        .setFilterByAuthorizedAccounts(false)
                        .build()
                )
                .build()

            oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener { result ->
                    try {
                        launcher.launch(
                            IntentSenderRequest.Builder(result.pendingIntent.intentSender)
                                .build()
                        )
                    } catch (e: Exception) {
                        viewModel.oneTapFailure(OneTapError.FailedToStartOneTap)
                    }
                }
                .addOnFailureListener { e ->
                    viewModel.oneTapFailure(OneTapError.FailedToLaunchOneTapUI(e))
                }

            viewModel.resetTriggerOneTap()
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
                        } else if (state.triggerOneTap is Resource.Error) {
                            DisplayError(
                                errorMessage = state.triggerOneTap.getErrorMessage(context),
                                dismissAction = viewModel::resetCreateOwnerResource,
                            ) { viewModel.retryCreateUser() }
                        }
                    }

                    else -> {
                        OwnerEntranceStandardUI(
                            authenticate = { viewModel.startOneTapFlow() }
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun OwnerEntranceStandardUI(
    authenticate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(onClick = authenticate) {
            Text(text = stringResource(R.string.one_tap_login))
        }
    }
}

sealed class OneTapError(val exception: Exception) {
    object InvalidToken : OneTapError(Exception("Invalid Token"))
    object MissingCredentialId : OneTapError(Exception("Missing Google Credential Id"))
    object UserCanceledOneTap : OneTapError(Exception("User Canceled One Tap"))
    object IntentResultFailed : OneTapError(Exception("Intent Result Failed"))
    object FailedToStartOneTap : OneTapError(Exception("Failed to Start One Tap"))
    data class ErrorParsingIntent(val e: Exception) : OneTapError(e)
    data class FailedToLaunchOneTapUI(val e: Exception) : OneTapError(e)
    data class FailedToVerifyId(val e: Exception) : OneTapError(e)
}