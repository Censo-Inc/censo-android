package co.censo.censo.presentation.plan_setup

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.GetLiveWithUserUI
import co.censo.censo.R
import co.censo.censo.presentation.VaultColors
import co.censo.censo.presentation.facetec_auth.FacetecAuth
import co.censo.censo.presentation.plan_setup.components.ActivateApproverUI
import co.censo.censo.presentation.plan_setup.components.ApproverNicknameUI
import co.censo.censo.presentation.plan_setup.components.AddAlternateApproverUI
import co.censo.censo.presentation.plan_setup.components.AddTrustedApproversUI
import co.censo.censo.presentation.plan_setup.components.SavedAndShardedUI
import co.censo.shared.presentation.cloud_storage.CloudStorageHandler
import co.censo.shared.util.projectLog
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanSetupScreen(
    navController: NavController,
    welcomeFlow: Boolean = false,
    viewModel: PlanSetupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state = viewModel.state

    val iconPair = when (state.backArrowType) {
        PlanSetupState.BackIconType.Back -> Icons.Filled.ArrowBack to R.string.back
        PlanSetupState.BackIconType.Exit -> Icons.Filled.Clear to R.string.exit
        else -> null
    }

    LaunchedEffect(key1 = state) {
        if (state.navigationResource is Resource.Success) {
            state.navigationResource.data?.let {
                navController.navigate(it)
                viewModel.resetNavigationResource()
            }
        }
    }

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                viewModel.onStart(welcomeFlow)
            }
            Lifecycle.Event.ON_PAUSE -> {
                viewModel.onStop()
            }
            else -> Unit
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            colors = TopAppBarDefaults.smallTopAppBarColors(
                containerColor = VaultColors.NavbarColor
            ),
            navigationIcon = {
                when (iconPair) {
                    null -> {}
                    else -> {
                        IconButton(onClick = viewModel::onBackClicked) {
                            Icon(
                                imageVector = iconPair.first,
                                contentDescription = stringResource(id = iconPair.second),
                            )
                        }
                    }
                }
            },
            title = {
                Text(
                    text =
                    when (state.planSetupUIState) {
                        PlanSetupUIState.ApproverActivation,
                        PlanSetupUIState.EditApproverNickname ->
                            if (state.approverType == ApproverType.Primary) {
                                stringResource(id = R.string.primary_approver)
                            } else {
                                stringResource(id = R.string.alternate_approver)
                            }

                        else -> ""
                    }
                )
            },
            actions = {
                IconButton(onClick = {
                    Toast.makeText(context, "Show FAQ Web View", Toast.LENGTH_LONG).show()
                }) {
                    Icon(
                        painterResource(id = co.censo.shared.R.drawable.question),
                        contentDescription = "learn more"
                    )
                }
            })
    }) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            when {
                state.loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(72.dp),
                        strokeWidth = 5.dp
                    )
                }

                state.asyncError -> {
                    // FIXME add error cases with appropriate actions
                    DisplayError(
                        errorMessage = "Failed to invite approvers",
                        dismissAction = { viewModel.reset() },
                        retryAction = { viewModel.reset() },
                    )
                }

                else -> {
                    when (state.planSetupUIState) {
                        PlanSetupUIState.Initial ->
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(72.dp),
                                strokeWidth = 5.dp
                            )

                        PlanSetupUIState.InviteApprovers ->
                            AddTrustedApproversUI(
                                welcomeFlow = welcomeFlow,
                                onInviteApproverSelected = { viewModel.onInviteApprover() },
                                onSkipForNowSelected = {
                                    viewModel.onFullyCompleted()
                                }
                            )

                        PlanSetupUIState.ApproverNickname -> {
                            ApproverNicknameUI(
                                nickname = state.editedNickname,
                                enabled = state.editedNicknameValid,
                                error = state.editedNicknameError,
                                onNicknameChanged = viewModel::approverNicknameChanged,
                                onSaveNickname = viewModel::onSaveApprover
                            )
                        }
                        
                        PlanSetupUIState.ApproverGettingLive -> {

                            GetLiveWithUserUI(
                                title = "${stringResource(R.string.get_live_with)} ${state.editedNickname}",
                                message = stringResource(R.string.get_live_with_your_approver_message),
                                onContinueLive = viewModel::onGoLiveWithApprover,
                                onResumeLater = viewModel::onBackClicked
                            )
                        }

                        PlanSetupUIState.ApproverActivation -> {
                            ActivateApproverUI(
                                isPrimaryApprover = state.approverType == ApproverType.Primary,
                                prospectApprover = state.activatingApprover,
                                secondsLeft = state.secondsLeft,
                                verificationCode = state.approverCodes[state.activatingApprover?.participantId] ?: "",
                                storesLink = "https://censo.co/approvers",
                                onContinue = viewModel::onApproverConfirmed,
                                onEditNickname = viewModel::onEditApproverNickname
                            )
                        }

                        PlanSetupUIState.EditApproverNickname -> {
                            ApproverNicknameUI(
                                isRename = true,
                                nickname = state.editedNickname,
                                enabled = state.editedNicknameValid,
                                error = state.editedNicknameError,
                                onNicknameChanged = viewModel::approverNicknameChanged,
                                onSaveNickname = viewModel::onSaveApproverNickname
                            )
                        }

                        PlanSetupUIState.AddAlternateApprover -> {
                            AddAlternateApproverUI(
                                onInviteAlternateSelected = viewModel::onInviteApprover,
                                onSaveAndFinishSelected = viewModel::saveAndFinish
                            )
                        }

                        PlanSetupUIState.RecoveryInProgress -> {
                            FacetecAuth(
                                onFaceScanReady = { verificationId, biometry ->
                                    viewModel.onFaceScanReady(verificationId, biometry)
                                },
                                onCancelled = { viewModel.onBackClicked() }
                            )
                        }

                        PlanSetupUIState.Completed -> {
                            val secrets = state.ownerState?.vault?.secrets
                            SavedAndShardedUI(
                                seedPhraseNickname = when {
                                    secrets.isNullOrEmpty() -> null
                                    secrets.size == 1 -> secrets.first().label
                                    else -> stringResource(
                                        id = R.string.count_seed_phrases,
                                        secrets.size
                                    )
                                },
                                primaryApproverNickname = state.primaryApprover?.label,
                                alternateApproverNickname = state.alternateApprover?.label,
                            )

                            LaunchedEffect(Unit) {
                                delay(8000)
                                viewModel.onFullyCompleted()
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.cloudStorageAction.triggerAction) {
        val encryptedKey = viewModel.getPrivateKeyForUpload()
        val participantId = state.tempOwnerApprover?.participantId

        if (encryptedKey != null && participantId != null) {
            CloudStorageHandler(
                actionToPerform = state.cloudStorageAction.action,
                participantId = participantId,
                privateKey = encryptedKey,
                onActionSuccess = { viewModel.onKeyUploadSuccess() },
                onActionFailed = viewModel::onKeyUploadFailed
            )
        } else {
            val exceptionCause =
                if (encryptedKey == null) "missing private key" else "missing participant id"
            viewModel.onKeyUploadFailed(Exception("Unable to setup initial policy, $exceptionCause"))
        }
    }
}