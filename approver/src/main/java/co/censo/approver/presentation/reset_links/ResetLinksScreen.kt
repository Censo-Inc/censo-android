package co.censo.approver.presentation.reset_links

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.approver.R
import co.censo.approver.presentation.reset_links.components.GetLiveWithOwnerUI
import co.censo.approver.presentation.reset_links.components.ListOwnersUI
import co.censo.approver.presentation.reset_links.components.ShareLinkUI
import co.censo.shared.data.Resource
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.LargeLoading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetLinksScreen(
    navController: NavController,
    viewModel: ResetLinksViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> viewModel.onStart()
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        navController.previousBackStackEntry?.let { navController.popBackStack() }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(id = R.string.close),
                        )
                    }
                },
                title = {
                    Text(text = stringResource(R.string.login_assistance))
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(color = Color.White),
        ) {
            when {
                state.loading -> {
                    LargeLoading(
                        color = SharedColors.DefaultLoadingColor,
                        fullscreen = true,
                        fullscreenBackgroundColor = Color.White
                    )
                }

                state.asyncError -> {
                    if (state.userResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = state.userResponse.getErrorMessage(context),
                            dismissAction = null,
                            retryAction = viewModel::retrieveApproverState,
                        )
                    }
                    if (state.createResetTokenResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = state.createResetTokenResponse.getErrorMessage(context),
                            dismissAction = null,
                            retryAction = viewModel::retrieveApproverState,
                        )
                    }
                }

                else -> {

                    when (val uiState = state.uiState) {
                        is ResetLinksState.ResetLinkUIState.ListApprovers -> {
                            ListOwnersUI(
                                approverStates = state.approverStates,
                                selectedParticipantId = uiState.selectedParticipantId,
                                onParticipantIdSelected = { participantId ->
                                    viewModel.onParticipantIdSelected(participantId)
                                },
                                onContinue = {
                                    uiState.selectedParticipantId?.let { viewModel.continueToGetLiveWithOwner(it) }
                                },
                            )
                        }

                        is ResetLinksState.ResetLinkUIState.GettingLive -> {
                            GetLiveWithOwnerUI(
                                onContinue = { viewModel.onGettingLive(uiState.participantId) }
                            )
                        }

                        is ResetLinksState.ResetLinkUIState.ShareLink -> {
                            ShareLinkUI(
                                link = uiState.resetToken.deeplink()
                            )
                        }
                    }
                }
            }
        }
    }
}