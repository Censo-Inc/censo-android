package co.censo.vault.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.vault.R
import co.censo.vault.presentation.Screen
import co.censo.vault.presentation.vault.VaultScreenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainVaultScreen(
    navController: NavController,
    viewModel: VaultScreenViewModel = hiltViewModel()
) {
    val state = viewModel.state

    val selectedItem = remember {
        mutableStateOf<BottomNavItem>(BottomNavItem.Home)
    }

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose { }
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
        }

        if (state.triggerDeleteUserDialog is Resource.Success) {
            HomeDialog(
                title = stringResource(R.string.you_are_about_to_delete_user),
                onCancel = viewModel::onCancelResetUser,
                onDelete = viewModel::deleteUser,
            )
        }

        if (state.triggerEditPhraseDialog is Resource.Success) {
            HomeDialog(
                title = stringResource(R.string.you_are_about_to_delete_phrase),
                onCancel = viewModel::onCancelDeletePhrase,
                onDelete = viewModel::deleteSecret,
            )
        }
    }
}

@Composable
fun HomeDialog(
    title: String,
    onCancel: () -> Unit, onDelete: () -> Unit
) {
    AlertDialog(onDismissRequest = onCancel, text = {
        Text(
            modifier = Modifier.padding(8.dp),
            text = title,
            color = Color.Black,
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal
        )
    }, confirmButton = {
        Button(
            onClick = onDelete
        ) {
            Text(stringResource(R.string.confirm))
        }
    }, dismissButton = {
        Button(
            onClick = onCancel
        ) {
            Text(stringResource(R.string.cancel))
        }
    })
}