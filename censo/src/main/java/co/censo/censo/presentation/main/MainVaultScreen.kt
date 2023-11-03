package co.censo.censo.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.shared.SharedScreen
import co.censo.shared.data.Resource
import co.censo.shared.presentation.components.DisplayError
import co.censo.censo.R
import co.censo.censo.presentation.Screen
import co.censo.censo.util.popUpToTop

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
            navController.navigate(SharedScreen.EntranceRoute.route) {
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
                state.loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(72.dp)
                            .align(Alignment.Center),
                        strokeWidth = 8.dp,
                        color = Color.Black
                    )
                }

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
                    }
                }

                else -> {


                    when (selectedItem.value) {
                        BottomNavItem.Home ->
                            VaultHomeScreen(
                                seedPhrasesSaved = state.secretsSize,
                                approvers = state.externalApprovers,
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
                                    navController.navigate(Screen.PlanSetupRoute.buildNavRoute(false))
                                }
                            )

                        BottomNavItem.Settings ->
                            SettingsHomeScreen(
                                onDeleteUser = viewModel::showDeleteUserDialog,
                                onSignOut = {},
                            )
                    }


                    if (state.triggerDeleteUserDialog is Resource.Success) {
                        HomeDialog(
                            title = stringResource(id = R.string.delete_user),
                            message = stringResource(R.string.you_are_about_to_delete_user),
                            onCancel = viewModel::onCancelResetUser,
                            onDelete = viewModel::deleteUser,
                        )
                    }

                    if (state.triggerEditPhraseDialog is Resource.Success) {
                        HomeDialog(
                            title = stringResource(id = R.string.delete_phrase),
                            message = stringResource(R.string.you_are_about_to_delete_phrase),
                            onCancel = viewModel::onCancelDeletePhrase,
                            onDelete = viewModel::deleteSecret,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeDialog(
    title: String,
    message: String,
    onCancel: () -> Unit, onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                modifier = Modifier.padding(8.dp),
                text = title,
                color = Color.Black,
                textAlign = TextAlign.Center,
                fontSize = 24.sp,
                fontWeight = FontWeight.W500,
            )
        },
        text = {
            Text(
                modifier = Modifier.padding(8.dp),
                text = message,
                color = Color.Black,
                textAlign = TextAlign.Start,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal
            )
        }, confirmButton = {
            TextButton(
                onClick = onDelete
            ) {
                Text(
                    stringResource(R.string.confirm),
                    color = Color.Black,
                    fontSize = 20.sp
                )
            }
        }, dismissButton = {
            TextButton(
                onClick = onCancel
            ) {
                Text(
                    stringResource(R.string.cancel),
                    color = Color.Black,
                    fontSize = 20.sp
                )
            }
        }
    )
}