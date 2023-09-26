package co.censo.vault.presentation.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.vault.R
import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.util.projectLog
import co.censo.vault.presentation.components.OnLifecycleEvent
import co.censo.vault.presentation.owner_ready.OwnerReadyScreen

@Composable
fun HomeScreen(
    navController: NavController, viewModel: HomeViewModel = hiltViewModel()
) {

    val state = viewModel.state

    val context = LocalContext.current as FragmentActivity

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    fun checkPermissionDialog() {
        try {
            val notificationGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                )

            val shownPermissionJustOnceBefore =
                ActivityCompat.shouldShowRequestPermissionRationale(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                )

            val seenDialogBefore = viewModel.userHasSeenPushDialog()

            if (notificationGranted != PackageManager.PERMISSION_GRANTED) {
                if (shownPermissionJustOnceBefore && !seenDialogBefore) {
                    viewModel.setUserSeenPushDialog(true)
                    viewModel.triggerPushNotificationDialog()
                } else if (!seenDialogBefore) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

        } catch (e: Exception) {
            projectLog(message = "checkPermissionDialog exception caught: ${e.message}")
            //TODO: Log exception
        }
    }

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                checkPermissionDialog()
            }
            else -> Unit
        }
    }

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose {}
    }

    when {
        state.loading -> {
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

        state.asyncError -> {
            if (state.ownerStateResource is Resource.Error) {
                DisplayError(
                    errorMessage = state.ownerStateResource.getErrorMessage(context),
                    dismissAction = null,
                    retryAction = viewModel::retrieveOwnerState
                )
            }
        }

        else -> {
            state.ownerStateResource.data?.also { ownerState ->
                when (ownerState) {
                    is OwnerState.GuardianSetup, OwnerState.Initial -> {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .background(color = Color.White),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            TextButton(
                                onClick = { navController.navigate(Screen.GuardianInvitationRoute.route) },
                            ) {
                                Row() {
                                    Icon(
                                        imageVector = Icons.Rounded.Groups,
                                        contentDescription = "Setup Policy",
                                        tint = Color.Black
                                    )
                                    Text(
                                        text = "Setup policy",
                                        color = Color.Black
                                    )
                                }
                            }

                            if (state.showPushNotificationsDialog is Resource.Success) {
                                PushNotificationDialog(
                                    text = stringResource(id = R.string.push_notification_never_dialog),
                                    onAccept = {
                                        viewModel.resetPushNotificationDialog()
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                    onDismiss = {
                                        viewModel.setUserSeenPushDialog(false)
                                        viewModel.resetPushNotificationDialog()
                                    }
                                )
                            }
                        }
                    }
                    is OwnerState.Ready -> {
                        OwnerReadyScreen(
                            ownerState,
                            refreshOwnerState = viewModel::retrieveOwnerState,
                            updateOwnerState = viewModel::updateOwnerState,
                        )
                    }
                }
            }
        }
    }
}

