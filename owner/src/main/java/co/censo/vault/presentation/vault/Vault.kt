package co.censo.vault.presentation.vault

import android.annotation.SuppressLint
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.shared.data.model.OwnerState
import co.censo.vault.presentation.home.Screen
import co.censo.vault.util.TestTag

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun VaultSecrets(
    ownerState: OwnerState.Ready,
    updateOwnerState: (OwnerState) -> Unit,
    navController: NavController,
    viewModel: VaultViewModel = hiltViewModel()
) {

    val state = viewModel.state

    DisposableEffect(key1 = viewModel) {
        viewModel.onNewOwnerState(ownerState)
        onDispose { }
    }

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

                if (!viewModel.isLocked(ownerState)) {
                    TextButton(
                        onClick = { navController.navigate("${Screen.AddBIP39Route.route}/${state.ownerState!!.vault.publicMasterEncryptionKey.value}") },
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
            }

            LazyColumn(
                modifier = Modifier
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val secrets = state.ownerState?.vault?.secrets ?: listOf()

                items(secrets.size) { index ->
                    VaultSecret(
                        secret = secrets[index],
                        isLocked = viewModel.isLocked(ownerState),
                        onDelete = { viewModel.deleteSecret(it, updateOwnerState) }
                    )
                }
            }
        }
    }
}


