package co.censo.censo.presentation.main

import android.app.Activity
import android.content.IntentSender
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.censo.R
import co.censo.censo.presentation.Screen
import co.censo.censo.presentation.components.DeleteUserConfirmationUI
import co.censo.censo.presentation.enter_phrase.components.AddPhraseLabelUI
import co.censo.censo.presentation.push_notification.PushNotificationScreen
import co.censo.shared.data.Resource
import co.censo.shared.data.model.AccessIntent
import co.censo.shared.data.model.TimelockSetting
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.components.ConfirmationDialog
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.LargeLoading
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.GoogleAuth
import co.censo.shared.util.popUpToTop
import co.censo.shared.util.sendError
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainVaultScreen(
    selectedBottomNavItem: MutableState<BottomNavItem>,
    navController: NavController,
    viewModel: VaultScreenViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current

    val showInfoView: MutableState<Boolean> = remember { mutableStateOf(false) }

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> viewModel.onStart()
            Lifecycle.Event.ON_RESUME -> viewModel.onResume()
            Lifecycle.Event.ON_PAUSE -> viewModel.onPause()
            else -> Unit
        }
    }

    DisposableEffect(key1 = viewModel) {
        onDispose { }
    }

    val googleDriveAccessResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val authResult = Identity.getAuthorizationClient(context)
                    .getAuthorizationResultFromIntent(result.data)
                if (authResult.grantedScopes.contains(GoogleAuth.DRIVE_FILE_SCOPE.toString())) {
                    viewModel.setSyncCloudAccessMessage(SyncCloudAccessMessage.ACCESS_GRANTED)
                } else {
                    viewModel.setSyncCloudAccessMessage(SyncCloudAccessMessage.ACCESS_AUTH_FAILED)
                }
            } else {
                viewModel.setSyncCloudAccessMessage(SyncCloudAccessMessage.ACCESS_AUTH_FAILED)
            }
        }
    )

    LaunchedEffect(key1 = state) {
        if (state.kickUserOut is Resource.Success) {
            navController.navigate(Screen.EntranceRoute.route) {
                launchSingleTop = true
                popUpToTop()
            }
            viewModel.delayedReset()
        }

        if (state.resyncCloudAccessRequest) {
            val authorizationRequest = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(GoogleAuth.DRIVE_FILE_SCOPE))
                .build()

            Identity.getAuthorizationClient(context)
                .authorize(authorizationRequest)
                .addOnSuccessListener { authResult ->

                    //If the user can grant the access
                    if (authResult.hasResolution()) {
                        val pendingIntent = authResult.pendingIntent
                        try {
                            pendingIntent?.let { safeIntent ->
                                val intentSenderRequest = IntentSenderRequest.Builder(safeIntent.intentSender).build()
                                googleDriveAccessResultLauncher.launch(intentSenderRequest)
                            } ?: Exception("Pending Intent null").sendError(CrashReportingUtil.CloudStorageIntent)
                        } catch (e: IntentSender.SendIntentException) {
                            viewModel.setSyncCloudAccessMessage(SyncCloudAccessMessage.ACCESS_AUTH_FAILED)
                            e.sendError(CrashReportingUtil.CloudStorageIntent)
                        }
                    } else {
                        viewModel.setSyncCloudAccessMessage(SyncCloudAccessMessage.ALREADY_GRANTED)
                    }

                    viewModel.resetResyncCloudAccessRequest()
                }
                .addOnFailureListener {
                    viewModel.setSyncCloudAccessMessage(SyncCloudAccessMessage.ACCESS_AUTH_FAILED)
                    it.sendError(CrashReportingUtil.CloudStorageIntent)
                    viewModel.resetResyncCloudAccessRequest()
                }
        }

        if (state.syncCloudAccessMessage is Resource.Success) {
            val message = when (state.syncCloudAccessMessage.data) {
                SyncCloudAccessMessage.ALREADY_GRANTED -> {
                    context.getString(R.string.drive_access_already_granted)
                }
                SyncCloudAccessMessage.ACCESS_GRANTED -> {
                    context.getString(R.string.drive_access_granted)
                }
                SyncCloudAccessMessage.ACCESS_AUTH_FAILED -> {
                    context.getString(R.string.failed_to_authenticate_drive_access)
                }
                null -> null
            }

            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
            viewModel.resetSyncCloudAccessMessage()
        }
    }

    Scaffold(
        topBar = {
            VaultTopBar(
                bottomNavItem = selectedBottomNavItem.value,
                showClose = state.showClose,
                onDismiss = {
                    if (state.showRenamePhrase is Resource.Success) {
                        viewModel.resetShowRenamePhase()
                    }
                    if (showInfoView.value) {
                        showInfoView.value = false
                    } else {
                        viewModel.resetShowApproversUI()
                    }
                }
            )
        },
        bottomBar = {
            CensoBottomNavBar(selectedBottomNavItem.value) {
                if (it == BottomNavItem.Home) {
                    viewModel.resetShowApproversUI()
                }
                selectedBottomNavItem.value = it
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            contentAlignment = Alignment.Center,
        ) {

            when {
                state.loading -> LargeLoading(fullscreen = true)

                state.asyncError -> {
                    when {
                        state.userResponse is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.userResponse.getErrorMessage(context),
                                dismissAction = null,
                            ) { viewModel.retrieveOwnerState() }
                        }

                        state.deleteSeedPhraseResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.deleteSeedPhraseResource.getErrorMessage(
                                    context
                                ),
                                dismissAction = viewModel::resetDeleteSeedPhraseResponse,
                                retryAction = null,
                            )
                        }
                        state.updateSeedPhraseResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.updateSeedPhraseResource.getErrorMessage(
                                    context
                                ),
                                dismissAction = viewModel::resetUpdateSeedPhraseResponse,
                                retryAction = null,
                            )
                        }
                        state.deleteUserResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.deleteUserResource.getErrorMessage(context),
                                dismissAction = viewModel::resetDeleteUserResource,
                                retryAction = null
                            )
                        }

                        state.lockResponse is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.lockResponse.getErrorMessage(context),
                                dismissAction = viewModel::resetLockResource,
                                retryAction = viewModel::lock
                            )
                        }

                        state.deletePolicySetup is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.deletePolicySetup.getErrorMessage(context),
                                dismissAction = viewModel::resetDeletePolicySetupResource,
                                retryAction = null
                            )
                        }

                        state.enableTimelockResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.enableTimelockResource.getErrorMessage(context),
                                dismissAction = viewModel::resetEnableTimelockResource,
                                retryAction = null
                            )
                        }

                        state.disableTimelockResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.disableTimelockResource.getErrorMessage(context),
                                dismissAction = viewModel::resetDisableTimelockResource,
                                retryAction = null
                            )
                        }

                        state.cancelDisableTimelockResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.cancelDisableTimelockResource.getErrorMessage(context),
                                dismissAction = viewModel::resetCancelDisableTimelockResource,
                                retryAction = null
                            )
                        }

                        state.cancelAccessResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.cancelAccessResource.getErrorMessage(context),
                                dismissAction = viewModel::resetCancelAccess,
                                retryAction = null
                            )
                        }

                        state.showAddApproversUI is Resource.Error -> {
                            DisplayError(
                                errorMessage = stringResource(id = R.string.cannot_add_approvers),
                                dismissAction = viewModel::resetShowApproversUI,
                                retryAction = null
                            )
                        }

                        state.removeApprovers is Resource.Error -> {
                            DisplayError(
                                errorMessage = stringResource(id = R.string.cannot_remove_approvers),
                                dismissAction = viewModel::resetRemoveApprovers,
                                retryAction = null
                            )
                        }
                    }
                }

                else -> {
                    when (selectedBottomNavItem.value) {
                        BottomNavItem.Home -> {
                            VaultHomeScreen(
                                seedPhrasesSaved = state.seedPhrasesSize,
                                approverSetupExists = state.ownerState?.policySetup != null,
                                approvers = state.ownerState?.policy?.approvers ?: emptyList(),
                                onAddSeedPhrase = {
                                    state.ownerState?.vault?.publicMasterEncryptionKey?.let { masterPublicKey ->
                                        val route = Screen.EnterPhraseRoute.buildNavRoute(
                                            masterPublicKey = masterPublicKey,
                                            welcomeFlow = false
                                        )
                                        navController.navigate(route)
                                    }
                                },
                                onAddApprovers = viewModel::showAddApproverUI,
                            )

                            if (state.showAddApproversUI is Resource.Success) {
                                SetupApproversScreen(
                                    approverSetupExists = state.ownerState?.policySetup != null,
                                    isInfoViewVisible = showInfoView.value,
                                    onInviteApproversSelected = {
                                        viewModel.resetShowApproversUI()
                                        navController.navigate(viewModel.determinePolicyModificationRoute())
                                    },
                                    onCancelApproverOnboarding = {
                                        viewModel.showDeletePolicySetupConfirmationDialog()
                                    },
                                    onShowInfoView = {
                                        showInfoView.value = true
                                    }
                                )

                                if (state.showDeletePolicySetupConfirmationDialog) {
                                    ConfirmationDialog(
                                        title = stringResource(id = R.string.are_you_sure),
                                        message = stringResource(R.string.approvers_activation_progress_made_so_far_will_be_lost),
                                        onCancel = viewModel::hideDeletePolicySetupConfirmationDialog,
                                        onDelete = viewModel::deletePolicySetupConfirmed
                                    )
                                }
                            }
                        }

                        BottomNavItem.Phrases ->
                            if (state.showRenamePhrase is Resource.Success) {
                                AddPhraseLabelUI(
                                    label = state.label,
                                    enabled = state.labelValid,
                                    labelIsTooLong = state.labelIsTooLong,
                                    onLabelChanged = viewModel::updateLabel,
                                    onSavePhrase = viewModel::renameSeedPhrase,
                                    isRename = true
                                )
                            } else {
                                PhraseHomeScreen(
                                    seedPhrases = state.ownerState?.vault?.seedPhrases
                                        ?: emptyList(),
                                    onAddClick = {
                                        state.ownerState?.vault?.publicMasterEncryptionKey?.let { masterPublicKey ->
                                            val route = Screen.EnterPhraseRoute.buildNavRoute(
                                                masterPublicKey = masterPublicKey,
                                                welcomeFlow = false
                                            )
                                            navController.navigate(route)
                                        }
                                    },
                                    onAccessClick = {
                                        navController.navigate(
                                            Screen.AccessApproval.withIntent(
                                                intent = AccessIntent.AccessPhrases
                                            )
                                        )
                                    },
                                    onRenamePhraseClick = { seedPhrase ->
                                        viewModel.showRenamePhrase(seedPhrase)
                                    },
                                    onDeletePhraseClick = { seedPhrase ->
                                        viewModel.showDeletePhraseDialog(seedPhrase)
                                    },
                                    onCancelAccessClick = viewModel::onCancelAccess,
                                    accessButtonLabel = viewModel.determineAccessButtonLabel(),
                                    timelockExpiration = viewModel.accessTimelockExpiration(),
                                    accessButtonEnabled = viewModel.accessButtonEnabled()
                                )
                            }

                        BottomNavItem.Settings ->
                            if (state.showPushNotificationsUI is Resource.Success) {
                                PushNotificationScreen(onFinished = viewModel::resetShowPushNotificationsUI)
                            } else {
                                SettingsHomeScreen(
                                    onResyncCloudAccess = viewModel::resyncCloudAccess,
                                    onLock = viewModel::lock,
                                    onDeleteUser = viewModel::showDeleteUserDialog,
                                    onSignOut = viewModel::signOut,
                                    showRemoveApproverButton = (state.ownerState?.policy?.approvers?.size ?: 1) > 1,
                                    onRemoveApprover = {
                                        if (state.ownerState?.hasBlockingPhraseAccessRequest() == true) {
                                            viewModel.setRemoveApproversError()
                                        } else {
                                            navController.navigate(
                                                Screen.AccessApproval.withIntent(
                                                    intent = AccessIntent.ReplacePolicy
                                                )
                                            )
                                        }
                                    },
                                    onShowPushNotification = viewModel::showPushNotificationsUI,
                                    showNotificationsButton = !viewModel.userHasSeenPushDialog(),
                                    onEnableTimelock = viewModel::enableTimelock,
                                    onDisableTimelock = viewModel::disableTimelock,
                                    onCancelDisableTimelock = viewModel::onCancelDisableTimelock,
                                    timelockSetting = viewModel.state.ownerState?.timelockSetting ?: TimelockSetting(0L, null,  null)
                                )
                            }
                    }


                    if (state.triggerDeleteUserDialog is Resource.Success) {
                        DeleteUserConfirmationUI(
                            title = stringResource(id = R.string.delete_user),
                            seedCount = state.ownerState?.vault?.seedPhrases?.size ?: 0,
                            onCancel = viewModel::onCancelResetUser,
                            onDelete = viewModel::deleteUser,
                        )
                    }

                    if (state.triggerDeletePhraseDialog is Resource.Success) {
                        val confirmationText = stringResource(R.string.delete_phrase_confirmation_text, state.triggerDeletePhraseDialog.data.label)

                        val message = buildAnnotatedString {
                            append(stringResource(R.string.you_are_about_to_delete_phrase))
                            append(confirmationText)
                        }

                        ConfirmationDialog(
                            title = stringResource(id = R.string.delete_phrase),
                            message = message,
                            confirmationText = confirmationText,
                            onCancel = viewModel::onCancelDeletePhrase,
                            onDelete = viewModel::deleteSeedPhrase,
                        )
                    }

                    if (state.triggerCancelDisableTimelockDialog is Resource.Success) {

                        val message = buildAnnotatedString {
                            append(stringResource(R.string.cancel_disable_timelock_confirmation))
                        }

                        ConfirmationDialog(
                            title = stringResource(id = R.string.cancel_disable_timelock),
                            message = message,
                            onCancel = viewModel::resetCancelDisableTimelockDialog,
                            onDelete = viewModel::cancelDisableTimelock,
                        )
                    }

                    if (state.triggerCancelAccessDialog is Resource.Success) {

                        val message = buildAnnotatedString {
                            append(stringResource(R.string.cancel_access_dialog_confirmation))
                        }

                        ConfirmationDialog(
                            title = stringResource(id = R.string.cancel_access),
                            message = message,
                            onCancel = viewModel::resetCancelAccess,
                            onDelete = viewModel::cancelAccess,
                        )
                    }
                }
            }
        }
    }
}
