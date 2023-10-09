package co.censo.vault.presentation.vault

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.presentation.components.DisplayError
import co.censo.vault.presentation.components.vault.UnlockedVaultScreen
import co.censo.vault.presentation.components.vault.VaultSecretListItem
import co.censo.vault.presentation.home.Screen
import co.censo.vault.util.TestTag

enum class VaultScreens {
    Unlocked, EditSeedPhrases
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun VaultScreen(
    navController: NavController,
    viewModel: VaultScreenViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose { }
    }

    LaunchedEffect(key1 = state) {
        if (state.navigationResource is Resource.Success) {
            state.navigationResource.data?.let {
                navController.navigate(it)
            }
            viewModel.reset()
        }
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
            when {
                state.ownerStateResource is Resource.Error -> {
                    DisplayError(
                        errorMessage = state.ownerStateResource.getErrorMessage(context),
                        dismissAction = null,
                    ) { viewModel.retrieveOwnerState() }
                }

                state.storeSeedPhraseResource is Resource.Error -> {
                    DisplayError(
                        errorMessage = state.storeSeedPhraseResource.getErrorMessage(context),
                        dismissAction = viewModel::resetStoreSeedPhraseResponse,
                    ) {}
                }

                state.deleteSeedPhraseResource is Resource.Error -> {
                    DisplayError(
                        errorMessage = state.deleteSeedPhraseResource.getErrorMessage(context),
                        dismissAction = viewModel::resetDeleteSeedPhraseResponse,
                    ) {}
                }
            }
        }

        else -> {

            when (state.screen) {
                VaultScreens.Unlocked -> {
                    UnlockedVaultScreen(
                        onEditSeedPhrases = viewModel::onEditSeedPhrases,
                        onRecoverSeedPhrases = viewModel::onRecoverPhrases
                    )
                }

                VaultScreens.EditSeedPhrases -> {
                    state.ownerStateResource.data?.let { ownerState ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .border(1.dp, Color.Gray)
                        ) {
                            Column(
                                Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Top,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {

                                Spacer(Modifier.height(24.dp))

                                Row(
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "Vault",
                                        modifier = Modifier.padding(start = 24.dp),
                                        textAlign = TextAlign.Center,
                                        color = Color.Black,
                                        fontSize = 36.sp
                                    )

                                    Spacer(modifier = Modifier.weight(1f))

                                    TextButton(
                                        onClick = { navController.navigate("${Screen.AddBIP39Route.route}/${ownerState.vault.publicMasterEncryptionKey.value}") },
                                        modifier = Modifier
                                            .semantics { testTag = TestTag.add_phrase }
                                            .padding(end = 8.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AddCircle,
                                            modifier = Modifier.padding(end = 18.dp),
                                            contentDescription = "Unlock",
                                            tint = Color.Black
                                        )
                                    }
                                }

                                LazyColumn(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val secrets = ownerState.vault.secrets

                                    items(secrets.size) { index ->
                                        VaultSecretListItem(
                                            secret = secrets[index],
                                            onDelete = {
                                                viewModel.deleteSecret(it)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


