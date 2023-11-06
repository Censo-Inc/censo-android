package co.censo.shared.presentation.cloud_storage

import Base58EncodedPrivateKey
import ParticipantId
import android.app.Activity
import android.content.IntentSender
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import co.censo.shared.R
import co.censo.shared.data.Resource
import co.censo.shared.presentation.SharedColors
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.GoogleAuth
import co.censo.shared.util.sendError
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity

@Composable
fun CloudStorageHandler(
    actionToPerform: CloudStorageActions,
    participantId: ParticipantId,
    privateKey: Base58EncodedPrivateKey?,
    onActionSuccess: (privateKey: Base58EncodedPrivateKey) -> Unit,
    onActionFailed: (exception: Exception?) -> Unit,
    onCloudStorageAccessGranted: (() -> Unit)? = null,
    viewModel: CloudStorageHandlerViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current

    var triggerAuthRequest by remember { mutableStateOf(false) }

    val googleDriveAccessResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val authResult = Identity.getAuthorizationClient(context)
                    .getAuthorizationResultFromIntent(result.data)
                if (authResult.grantedScopes.contains(GoogleAuth.DRIVE_FILE_SCOPE.toString())) {
                    viewModel.performAction(bypassScopeCheckForCloudStorage = true)
                }
            }
        }
    )

    LaunchedEffect(key1 = triggerAuthRequest) {
        if (triggerAuthRequest) {
            val authorizationRequest = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(GoogleAuth.DRIVE_FILE_SCOPE))
                .build()

            Identity.getAuthorizationClient(context)
                .authorize(authorizationRequest)
                .addOnSuccessListener { authResult ->
                    //If false then the user already granted permissions, we do not need to handle that
                    if (authResult.hasResolution()) {
                        val pendingIntent = authResult.pendingIntent
                        try {
                            pendingIntent?.let { safeIntent ->
                                val intentSenderRequest = IntentSenderRequest.Builder(safeIntent.intentSender).build()
                                googleDriveAccessResultLauncher.launch(intentSenderRequest)
                            } ?: Exception("Pending Intent null").sendError(CrashReportingUtil.CloudStorageIntent)
                        } catch (e: IntentSender.SendIntentException) {
                            e.sendError(CrashReportingUtil.CloudStorageIntent)
                        }
                    } else {
                        viewModel.performAction(bypassScopeCheckForCloudStorage = true)
                    }
                }
                .addOnFailureListener {
                    it.sendError(CrashReportingUtil.CloudStorageIntent)
                }

            triggerAuthRequest = !triggerAuthRequest
        }
    }

    LaunchedEffect(key1 = state) {
        if (state.cloudStorageActionResource is Resource.Success) {
            onActionSuccess(state.cloudStorageActionResource.data!!)
        }

        if (state.cloudStorageActionResource is Resource.Error) {
            onActionFailed(state.cloudStorageActionResource.exception)
        }

        if (state.cloudStorageAccessGranted) {
            onCloudStorageAccessGranted?.invoke()
        }
    }

    DisposableEffect(key1 =  viewModel) {
        viewModel.onStart(
            actionToPerform = actionToPerform,
            participantId = participantId,
            privateKey = privateKey
        )
        onDispose { viewModel.onDispose() }
    }

    //TODO: Update UI to match design style
    if (state.shouldEnforceCloudStorageAccess) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = SharedColors.DisabledGrey),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.google_drive_access_required),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { triggerAuthRequest = true }) {
                    Text(text = stringResource(R.string.grant_permission))
                }
            }
        }
    }
}