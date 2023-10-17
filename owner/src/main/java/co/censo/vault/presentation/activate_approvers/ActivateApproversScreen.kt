package co.censo.vault.presentation.activate_approvers

import StandardButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.components.DisplayError
import co.censo.vault.R
import co.censo.vault.presentation.Screen
import co.censo.vault.presentation.VaultColors
import co.censo.vault.presentation.components.ActivateApproverRow
import co.censo.vault.presentation.components.ActivateApproversTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivateApproversScreen(
    navController: NavController,
    viewModel: ActivateApproversViewModel = hiltViewModel()
) {

    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    LaunchedEffect(key1 = state) {
        if (state.createPolicyResponse is Resource.Success) {
            navController.navigate(Screen.VaultScreen.route)
            viewModel.resetCreatePolicyResource()
        }
    }

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_START
            -> {
                viewModel.onStart()
            }
            Lifecycle.Event.ON_PAUSE -> {
                viewModel.onStop()
            }
            else -> Unit
        }
    }

    Scaffold(
        contentColor = Color.White,
        containerColor = Color.White,
        topBar = {
            ActivateApproversTopBar()
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .background(color = Color.White),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StandardButton(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = VaultColors.PrimaryColor,
                    borderColor = Color.White,
                    border = false,
                    contentPadding = PaddingValues(vertical = 12.dp),
                    onClick = { viewModel.createPolicy() },
                    enabled = state.guardians.all { it is Guardian.ProspectGuardian && it.status is GuardianStatus.Confirmed }
                )
                {
                    Text(
                        text = stringResource(id = R.string.continue_text),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.W700
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

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
                            color = VaultColors.PrimaryColor
                        )
                    }
                }

                state.asyncError -> {
                    if (state.userResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = state.userResponse.getErrorMessage(context),
                            dismissAction = viewModel::resetUserResponse,
                        ) {
                            viewModel.retrieveUserState()
                        }
                    } else if (state.createPolicyResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = state.createPolicyResponse.getErrorMessage(context),
                            dismissAction = viewModel::resetCreatePolicyResource,
                        ) {
                            viewModel.createPolicy()
                        }
                    } else if (state.setupError != null) {
                        DisplayError(
                            errorMessage = state.setupError,
                            dismissAction = viewModel::resetSetupError,
                        ) {
                            viewModel.createPolicy()
                        }
                    }
                }

                else -> {

                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.activate_approvers),
                            fontSize = 24.sp,
                            color = VaultColors.PrimaryColor,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        for (approver in state.guardians) {
                            ActivateApproverRow(
                                approver = approver,
                                approverCode = state.approverCodes[approver.participantId] ?: "",
                                percentageLeft = state.countdownPercentage,
                            )
                        }
                    }
                }
            }
        }
    }
}
