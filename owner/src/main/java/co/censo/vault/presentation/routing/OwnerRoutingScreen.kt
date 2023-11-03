package co.censo.vault.presentation.routing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.presentation.components.DisplayError

@Composable
fun OwnerRoutingScreen(
    navController: NavController,
    viewModel: OwnerRoutingViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current

    LaunchedEffect(key1 = state) {
        if (state.navigationResource is Resource.Success) {
            state.navigationResource.data?.let { destination ->
                navController.navigate(destination) {
                    popUpTo(destination) {
                        inclusive = true
                    }
                }
            }
            viewModel.resetNavigationResource()
        }
    }


    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose { viewModel.onStop() }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (state.userResponse is Resource.Error) {
            DisplayError(
                errorMessage = state.userResponse.getErrorMessage(context),
                dismissAction = { viewModel.retrieveOwnerState(false) },
                retryAction = { viewModel.retrieveOwnerState(false) },
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.size(44.dp))
        }
    }
}
