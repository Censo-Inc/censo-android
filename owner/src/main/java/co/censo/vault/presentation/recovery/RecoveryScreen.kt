package co.censo.vault.presentation.recovery

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.data.model.Recovery
import co.censo.shared.presentation.components.DisplayError
import co.censo.vault.presentation.VaultColors
import co.censo.vault.presentation.components.recovery.AnotherDeviceRecoveryScreen
import co.censo.vault.presentation.components.recovery.ThisDeviceRecoveryScreen

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun RecoveryScreen(
    navController: NavController,
    viewModel: RecoveryScreenViewModel = hiltViewModel()
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

        if (state.initiateNewRecovery) {
            viewModel.initiateRecovery()
        }
    }

    when {
        state.loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = VaultColors.PrimaryColor)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(72.dp)
                        .align(Alignment.Center),
                    strokeWidth = 8.dp,
                    color = Color.White
                )
            }
        }

        state.asyncError -> {
            when {
                state.userResponse is Resource.Error -> {
                    DisplayError(
                        errorMessage = state.userResponse.getErrorMessage(context),
                        dismissAction = null,
                    ) { viewModel.reloadOwnerState() }
                }

                state.initiateRecoveryResource is Resource.Error -> {
                    DisplayError(
                        errorMessage = state.initiateRecoveryResource.getErrorMessage(context),
                        dismissAction = null,
                    ) { viewModel.initiateRecovery() }
                }

                state.cancelRecoveryResource is Resource.Error -> {
                    DisplayError(
                        errorMessage = state.cancelRecoveryResource.getErrorMessage(context),
                        dismissAction = null,
                    ) { viewModel.cancelRecovery() }
                }
            }
        }

        else -> {

            when (val recovery = state.recovery) {
                null -> {
                    // recovery is about to be requested
                }

                is Recovery.AnotherDevice -> {
                    AnotherDeviceRecoveryScreen()
                }

                is Recovery.ThisDevice -> {
                    ThisDeviceRecoveryScreen(
                        recovery,
                        guardians = state.guardians,
                        approvalsRequired = state.approvalsRequired,
                        approvalsCollected = state.approvalsCollected,
                        onCancelRecovery = viewModel::cancelRecovery,
                        context = context
                    )
                }
            }
        }
    }
}
