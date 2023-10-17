package co.censo.shared.presentation.entrance

import StandardButton
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.R
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.util.projectLog
import com.google.android.gms.auth.api.signin.GoogleSignIn

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun EntranceScreen(
    navController: NavController,
    guardianEntrance: Boolean,
    invitationId: String? = null,
    recoveryParticipantId: String? = null,
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
            viewModel.onGuardianStart(invitationId, recoveryParticipantId)
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
                val googleSignInClient = viewModel.getGoogleSignInClient()

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
                        color = Color.Black
                    )
                }
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
                        dismissAction = viewModel::resetSignInUserResource,
                    ) { viewModel.retrySignIn() }
                }
            }

            else -> {
                OwnerEntranceStandardUI(
                    authenticate = { viewModel.startGoogleSignInFlow() }
                )
            }
        }
    }
}

@Composable
fun OwnerEntranceStandardUI(
    authenticate: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Image(
            painter = painterResource(id = R.drawable.censo_text),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.sensible_crypto_security),
            fontWeight = FontWeight.W600,
            fontSize = 24.sp,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(24.dp))
        StandardButton(
            onClick = authenticate,
            contentPadding = PaddingValues(
                horizontal = 48.dp,
                vertical = 16.dp
            ),
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(id = R.drawable.google),
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.google_auth_login),
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(all = 10.dp)
        ) {
            Icon(
                Icons.Outlined.Info, contentDescription = null,
                Modifier
                    .height(height = 34.dp)
                    .padding(all = 8.dp)
            )
            Text(
                text = stringResource(R.string.why_google),
                fontSize = 20.sp,
                fontWeight = FontWeight.W500
            )
        }

        Spacer(modifier = Modifier.height(64.dp))

        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.weight(0.1f))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(0.7f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.eyeslash),
                    contentDescription = null,
                    modifier = Modifier.height(28.dp)
                )
                Text(
                    text = stringResource(R.string.no_personal_info),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(all = 12.dp)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(0.7f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.safe),
                    contentDescription = null,
                    modifier = Modifier.height(28.dp)
                )
                Text(
                    text = stringResource(R.string.multiple_layers),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(all = 12.dp)
                )
            }
            Spacer(modifier = Modifier.weight(0.1f))
        }
        Divider(modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp))
        Row(
            modifier = Modifier.padding(
                start = 32.dp,
                end = 32.dp,
                top = 20.dp,
                bottom = 32.dp
            )
        ) {
            Text(
                text = stringResource(R.string.terms),
                modifier = Modifier.clickable { uriHandler.openUri("https://censo.co/terms/") },
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(24.dp))
            Text(
                text = stringResource(R.string.privacy),
                modifier = Modifier.clickable { uriHandler.openUri("https://censo.co/privacy/") },
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Preview()
@Composable
fun OwnerEntranceStandardUIPreview() {
    Surface {
        OwnerEntranceStandardUI({})
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
    data class FailedToCreateKeyWithId(val e: Exception) : GoogleAuthError(e)
}