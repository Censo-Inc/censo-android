package co.censo.vault.presentation.recovery

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.data.model.Recovery
import co.censo.shared.presentation.Colors
import co.censo.shared.presentation.components.DisplayError
import co.censo.vault.presentation.components.recovery.AnotherDeviceRecoveryScreen
import co.censo.vault.presentation.components.recovery.RecoveryApprovalRow
import co.censo.vault.presentation.components.recovery.RequestingRecoveryScreen

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
                    ) { viewModel.reloadOwnerState() }
                }

                state.initiateRecoveryResource is Resource.Error -> {
                    DisplayError(
                        errorMessage = state.initiateRecoveryResource.getErrorMessage(context),
                        dismissAction = null,
                    ) { viewModel.initiateRecovery() }
                }
            }
        }

        else -> {

            when (val recovery = state.recovery) {
                null -> {
                    RequestingRecoveryScreen()
                }

                is Recovery.AnotherDevice -> {
                    AnotherDeviceRecoveryScreen()
                }

                is Recovery.ThisDevice -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .background(color = Colors.PrimaryBlue),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Recovery Initiated",
                            fontSize = 24.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        for (approval in recovery.approvals) {
                            RecoveryApprovalRow(
                                state.guardians.first { it.participantId == approval.participantId },
                                approval
                            )
                        }
                    }
                }
            }
        }
    }
}


