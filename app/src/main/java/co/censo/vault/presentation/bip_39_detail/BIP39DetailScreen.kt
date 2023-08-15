package co.censo.vault.presentation.bip_39_detail

import android.annotation.SuppressLint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.vault.R
import co.censo.vault.Resource
import co.censo.vault.presentation.components.VaultButton
import co.censo.vault.presentation.components.WriteWordUI


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BIP39DetailScreen(
    bip39Name: String,
    navController: NavController, viewModel: BIP39DetailViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart(bip39Name)
        onDispose {
            viewModel.reset()
        }
    }

    LaunchedEffect(key1 = state) {

        if (state.bioPromptTrigger is Resource.Success) {

            val promptInfo = BiometricUtil.createPromptInfo(context = context)

            val bioPrompt = BiometricUtil.createBioPrompt(
                fragmentActivity = context,
                onSuccess = {
                    viewModel.onBiometryApproved()
                },
                onFail = {
                    BiometricUtil.handleBioPromptOnFail(
                        context = context,
                        errorCode = it
                    ) {
                        viewModel.onBiometryFailed(errorCode = it)
                    }
                }
            )

            bioPrompt.authenticate(promptInfo)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.bip39_detail)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.leave_screen),
                            tint = Color.Black
                        )
                    }
                },
            )
        },
        content = {
            WriteWordUI(
                phrase = state.bip39Phrase,
                index = state.currentWordIndex,
                phraseSize = state.phraseWordCount,
                changeWordIndex = viewModel::wordIndexChanged,
                failedBiometry = state.bioPromptTrigger is Resource.Error,
                retry = { viewModel.onStart(bip39Name) }
            )
        }
    )

}