package co.censo.vault.presentation.owner_entrance

import android.annotation.SuppressLint
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.vault.R
import co.censo.shared.data.Resource
import co.censo.vault.BuildConfig
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerEntranceScreen(
    navController: NavController,
    viewModel: OwnerEntranceViewModel = hiltViewModel()
) {

    val context = LocalContext.current as FragmentActivity

    val oneTapClient: SignInClient = Identity.getSignInClient(context)
    lateinit var signInRequest: BeginSignInRequest

    val state = viewModel.state

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
                        Text(text = stringResource(R.string.owner_setup))
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
            Text(text = stringResource(R.string.onetap_login))
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

@Composable
fun DisplayError(
    errorMessage: String,
    dismissAction: () -> Unit,
    retryAction: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { dismissAction() },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(modifier = Modifier.padding(16.dp), text = errorMessage, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(18.dp))
        TextButton(onClick = retryAction) {
            Text(text = stringResource(R.string.retry))
        }
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = dismissAction) {
            Text(text = stringResource(R.string.dismiss))
        }
    }
}