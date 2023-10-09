package co.censo.vault.presentation.home

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.toSecurityPlan
import co.censo.shared.presentation.components.DisplayError
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {

    val state = viewModel.state

    val context = LocalContext.current as FragmentActivity

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose {}
    }

    LaunchedEffect(key1 = state) {
        if (state.ownerStateResource is Resource.Success) {
            if (state.ownerStateResource.data is OwnerState.Ready) {
                navController.navigate(Screen.VaultScreen.route)
                viewModel.resetOwnerState()
            }
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
            if (state.ownerStateResource is Resource.Error) {
                DisplayError(
                    errorMessage = state.ownerStateResource.getErrorMessage(context),
                    dismissAction = null,
                    retryAction = viewModel::retrieveOwnerState
                )
            }
        }

        else -> {
            state.ownerStateResource.data?.also { ownerState ->

                when (ownerState) {
                    is OwnerState.Initial -> {
                        EditingSecurityPlanUI {
                            navController.navigate(Screen.PlanSetupRoute.route)
                        }
                    }

                    is OwnerState.GuardianSetup -> {
                        if (state.userEditingPlan) {

                            val securityPlanEncode =
                                ownerState.toSecurityPlan()
                                    ?.let { "/${Uri.encode(Json.encodeToString(it))}" }
                                    ?: ""

                            EditingSecurityPlanUI {
                                navController.navigate("${Screen.PlanSetupRoute.route}${securityPlanEncode}")
                            }
                        } else {
                            ActivatingGuardiansUI {
                                navController.navigate(Screen.ActivateApprovers.route)
                            }
                        }
                    }

                    is OwnerState.Ready -> {}
                }
            }
        }
    }
}

@Composable
fun EditingSecurityPlanUI(
    navigateToPlanSetup: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(color = Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ClickableText(
            text = buildAnnotatedString { append("Setup Policy") },
            style = TextStyle(
                fontSize = 24.sp,
                color = Color.Black,
            ),
            onClick = { navigateToPlanSetup() }
        )
    }
}

@Composable
fun ActivatingGuardiansUI(
    navigateToActivateApprovers: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(color = Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ClickableText(
            text = buildAnnotatedString { append("Activate Approvers") },
            style = TextStyle(
                fontSize = 24.sp,
                color = Color.Black,
            ),
            onClick = { navigateToActivateApprovers() }
        )
    }
}
