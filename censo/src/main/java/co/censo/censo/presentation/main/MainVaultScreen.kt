package co.censo.censo.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.presentation.components.DisplayError
import co.censo.censo.R
import co.censo.censo.presentation.Screen
import co.censo.shared.presentation.components.ConfirmationDialog
import co.censo.shared.presentation.components.Loading
import co.censo.shared.util.popUpToTop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainVaultScreen(
    navController: NavController,
    viewModel: VaultScreenViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current

    val selectedItem = remember {
        mutableStateOf<BottomNavItem>(BottomNavItem.Home)
    }

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose { }
    }

    LaunchedEffect(key1 = state) {
        if (state.kickUserOut is Resource.Success) {
            navController.navigate(Screen.EntranceRoute.route) {
                launchSingleTop = true
                popUpToTop()
            }
            viewModel.reset()
        }
    }

    Scaffold(
        topBar = {
            VaultTopBar(selectedItem.value)
        },
        bottomBar = {
            CensoBottomNavBar(selectedItem.value) {
                selectedItem.value = it
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
                state.loading -> Loading(
                    strokeWidth = 8.dp,
                    color = Color.Black,
                    size = 72.dp,
                    fullscreen = true
                )

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
                    }
                }

                else -> {


                    when (selectedItem.value) {
                        BottomNavItem.Home ->
                            VaultHomeScreen(
                                seedPhrasesSaved = state.secretsSize,
                                onAddSeedPhrase = {
                                    state.ownerState?.vault?.publicMasterEncryptionKey?.let { masterPublicKey ->
                                        val route = Screen.EnterPhraseRoute.buildNavRoute(
                                            masterPublicKey = masterPublicKey,
                                            welcomeFlow = false
                                        )
                                        navController.navigate(route)
                                    }
                                },
                                onAddApprovers = {
                                    selectedItem.value = BottomNavItem.Approvers
                                },
                                showAddApprovers = state.externalApprovers == 0
                            )

                        BottomNavItem.Phrases ->
                            PhraseHomeScreen(
                                vaultSecrets = state.ownerState?.vault?.secrets ?: emptyList(),
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
                                    navController.navigate(Screen.AccessApproval.route)
                                },
                                onEditPhraseClick = { vaultSecret ->
                                    viewModel.showEditPhraseDialog(vaultSecret)
                                }
                            )

                        BottomNavItem.Approvers ->
                            ApproversHomeScreen(
                                approvers = state.ownerState?.policy?.guardians ?: emptyList(),
                                onInviteApproversSelected = {
                                    navController.navigate(Screen.PlanSetupRoute.route)
                                }
                            )

                        BottomNavItem.Settings ->
                            SettingsHomeScreen(
                                onLock = viewModel::lock,
                                onDeleteUser = viewModel::showDeleteUserDialog,
                                onSignOut = viewModel::signOut,
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
                            onDelete = viewModel::deleteSecret,
                        )
                    }
                }
            }
        }
    }
}