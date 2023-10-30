package co.censo.guardian.presentation.routing

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
import co.censo.guardian.presentation.Screen
import co.censo.guardian.presentation.home.GuardianUIState
import co.censo.shared.SharedScreen
import co.censo.shared.data.Resource
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.util.projectLog

@Composable
fun ApproverRoutingScreen(
    navController: NavController,
    viewModel: ApproverRoutingViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current

    LaunchedEffect(key1 = state) {
        if (state.navToGuardianHome is Resource.Success) {
            projectLog(tag = "Navigation", message = "Navving to GHVM")

            navController.navigate(SharedScreen.HomeRoute.route) {
                popUpTo(Screen.ApproverRoutingScreen.route) {
                    inclusive = true
                }
            }

            viewModel.resetGuardianHomeNavigationTrigger()
        }

        if (state.navToApproverOnboarding is Resource.Success) {
            navController.navigate(Screen.ApproverOnboardingScreen.route) {
                popUpTo(Screen.ApproverRoutingScreen.route) {
                    inclusive = true
                }
            }
            viewModel.resetApproverOnboardingNavigationTrigger()
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
                dismissAction = { viewModel.retrieveApproverState(false) }) {
                viewModel.retrieveApproverState(false)
            }
        } else {
            CircularProgressIndicator(modifier = Modifier.size(44.dp))
        }
    }
}
