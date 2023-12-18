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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.censo.R
import co.censo.censo.presentation.Screen
import co.censo.shared.data.Resource
import co.censo.shared.data.model.AccessIntent
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

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
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
            viewModel.reset()
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
                showCloseApprover = state.showAddApproversUI is Resource.Success,
                onDismissApprover = viewModel::resetShowApproversUI
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
                            ) {}
                        }

                        state.deleteUserResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.deleteUserResource.getErrorMessage(context),
                                dismissAction = viewModel::resetDeleteUserResource
                            ) { }
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
                                dismissAction = viewModel::resetDeletePolicySetupResource
                            ) { }
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
                                    onInviteApproversSelected = {
                                        viewModel.resetShowApproversUI()
                                        navController.navigate(viewModel.determinePolicyModificationRoute())
                                    },
                                    onCancelApproverOnboarding = {
                                        viewModel.showDeletePolicySetupConfirmationDialog()
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
                            PhraseHomeScreen(
                                seedPhrases = state.ownerState?.vault?.seedPhrases ?: emptyList(),
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
                                    navController.navigate(Screen.AccessApproval.withIntent(intent = AccessIntent.AccessPhrases))
                                },
                                onEditPhraseClick = { seedPhrase ->
                                    viewModel.showEditPhraseDialog(seedPhrase)
                                }
                            )

                        BottomNavItem.Settings ->
                            SettingsHomeScreen(
                                onResyncCloudAccess = viewModel::resyncCloudAccess,
                                onLock = viewModel::lock,
                                onDeleteUser = viewModel::showDeleteUserDialog,
                                onSignOut = viewModel::signOut,
                                onRemoveApprover = {
                                    navController.navigate(Screen.AccessApproval.withIntent(intent = AccessIntent.ReplacePolicy))
                                }
                            )
                    }


                    if (state.triggerDeleteUserDialog is Resource.Success) {

                        val annotatedString = buildAnnotatedString {
                            append(stringResource(id = R.string.about_to_delete_user_first_half))
                            append(" ")

                            withStyle(SpanStyle(fontWeight = FontWeight.W500)) {
                                append(stringResource(id = R.string.all))
                            }

                            append(" ")

                            append(stringResource(id = R.string.about_to_delete_user_second_half))
                        }

                        ConfirmationDialog(
                            title = stringResource(id = R.string.delete_user),
                            message = annotatedString,
                            onCancel = viewModel::onCancelResetUser,
                            onDelete = viewModel::deleteUser,
                        )
                    }

                    if (state.triggerEditPhraseDialog is Resource.Success) {

                        val message = buildAnnotatedString {
                            append(stringResource(R.string.you_are_about_to_delete_phrase))
                        }

                        ConfirmationDialog(
                            title = stringResource(id = R.string.delete_phrase),
                            message = message,
                            onCancel = viewModel::onCancelDeletePhrase,
                            onDelete = viewModel::deleteSeedPhrase,
                        )
                    }
                }
            }
        }
    }
}