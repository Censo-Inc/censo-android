package co.censo.vault.presentation.add_bip39

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.vault.R
import co.censo.vault.Resource
import co.censo.vault.presentation.components.VaultButton
import co.censo.vault.presentation.home.Screen


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBIP39Screen(
    navController: NavController, viewModel: AddBIP39ViewModel = hiltViewModel()
) {

    val state = viewModel.state

    LaunchedEffect(key1 = state) {
        if (state.submitStatus is Resource.Success) {
            navController.navigate(Screen.HomeRoute.route)
            viewModel.reset()
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text(text = stringResource(R.string.add_bip39_phrase)) })
    }, content = {
        Box(modifier = Modifier.padding(it)) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Text(text = stringResource(R.string.name))
                TextField(
                    value = state.name,
                    onValueChange = viewModel::updateName,
                    isError = state.nameError != null
                )
                if (state.nameError != null) {
                    Text(
                        text = state.nameError,
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(44.dp))

                Text(text = stringResource(R.string.bip39))
                TextField(
                    value = state.userEnteredPhrase,
                    onValueChange = viewModel::updateUserEnteredPhrase,
                    isError = state.userEnteredPhraseError != null
                )
                if (state.userEnteredPhraseError != null) {
                    Text(
                        text = state.userEnteredPhraseError,
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(44.dp))

                VaultButton(onClick = viewModel::submit, enabled = viewModel.canSubmit()) {
                    Text(text = stringResource(R.string.save))
                }
            }
        }
    })

}