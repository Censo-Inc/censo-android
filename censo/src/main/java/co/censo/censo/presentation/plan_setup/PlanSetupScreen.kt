package co.censo.censo.presentation.plan_setup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import co.censo.censo.presentation.plan_setup.components.SavedAndShardedUI
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.presentation.cloud_storage.CloudStorageHandler
import co.censo.shared.presentation.components.Loading
import co.censo.shared.util.LinksUtil
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanSetupScreen(
    navController: NavController,
    viewModel: PlanSetupViewModel = hiltViewModel()
) {
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
                viewModel.onStart()
            }
            Lifecycle.Event.ON_PAUSE -> {
                viewModel.onStop()
            }
            else -> Unit
        }
    }

    Scaffold(topBar = {
        TopAppBar(
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
                            stringResource(R.string.add_approver_title)

                        else -> ""
                    }
                )
            }
        )
    }) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            when {
                state.loading -> Loading(strokeWidth = 5.dp, size = 72.dp, fullscreen = true)


                state.asyncError -> {
                    if (state.verifyKeyConfirmationSignature is Resource.Error) {
                        DisplayError(
                            errorMessage = stringResource(R.string.cannot_verify_confirmation_signature),
                            dismissAction = { viewModel.resetVerifyKeyConfirmationSignature() },
                            retryAction = { viewModel.resetVerifyKeyConfirmationSignature() },
                        )
                    } else if (state.userResponse is Resource.Error){
                        DisplayError(
                            errorMessage = "User Response Error",
                            dismissAction = { viewModel.reset() },
                            retryAction = { viewModel.reset() },
                        )
                    } else if (state.createPolicySetupResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Create Policy Setup Error",
                            dismissAction = { viewModel.reset() },
                            retryAction = { viewModel.reset() },
                        )
                    } else if (state.initiateRecoveryResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Initiate Recovery Error",
                            dismissAction = { viewModel.reset() },
                            retryAction = { viewModel.reset() },
                        )
                    } else if (state.retrieveRecoveryShardsResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Retrieve Recovery Shards Error",
                            dismissAction = { viewModel.reset() },
                            retryAction = { viewModel.reset() },
                        )
                    } else if (state.replacePolicyResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Replace Policy Error",
                            dismissAction = { viewModel.reset() },
                            retryAction = { viewModel.reset() },
                        )
                    } else if (state.completeGuardianShipResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Complete Guardianship Error",
                            dismissAction = { viewModel.reset() },
                            retryAction = { viewModel.reset() },
                        )
                    } else {
                        DisplayError(
                            errorMessage = "Generic Error",
                            dismissAction = { viewModel.reset() },
                            retryAction = { viewModel.reset() },
                        )
                    }
                }

                else -> {
                    when (state.planSetupUIState) {
                        PlanSetupUIState.Initial -> Loading(
                            strokeWidth = 5.dp,
                            size = 72.dp,
                            fullscreen = true
                        )

                        PlanSetupUIState.ApproverNickname -> {
                            ApproverNicknameUI(
                                isFirstApprover = state.primaryApprover?.status !is GuardianStatus.Confirmed,
                                nickname = state.editedNickname,
                                enabled = state.editedNicknameValid,
                                nicknameIsTooLong = state.editedNicknameIsTooLong,
                                onNicknameChanged = viewModel::onApproverNicknameChanged,
                                onSaveNickname = viewModel::onSaveApprover
                            )
                        }
                        
                        PlanSetupUIState.ApproverGettingLive -> {
                            GetLiveWithUserUI(
                                title = "${stringResource(R.string.activate_approver)} ${state.editedNickname}",
                                message = stringResource(R.string.activate_your_approver_message, state.editedNickname),
                                activatingApprover = true,
                                onContinueLive = viewModel::onGoLiveWithApprover,
                                onResumeLater = viewModel::onBackClicked
                            )
                        }

                        PlanSetupUIState.ApproverActivation -> {
                            ActivateApproverUI(
                                prospectApprover = state.activatingApprover,
                                secondsLeft = state.secondsLeft,
                                verificationCode = state.approverCodes[state.activatingApprover?.participantId] ?: "",
                                storesLink = LinksUtil.CENSO_APPROVER_STORE_LINK,
                                onContinue = viewModel::onApproverConfirmed,
                                onEditNickname = viewModel::onEditApproverNickname
                            )
                        }

                        PlanSetupUIState.EditApproverNickname -> {
                            ApproverNicknameUI(
                                isFirstApprover = state.primaryApprover?.status !is GuardianStatus.Confirmed,
                                isRename = true,
                                nickname = state.editedNickname,
                                enabled = state.editedNicknameValid,
                                nicknameIsTooLong = state.editedNicknameIsTooLong,
                                onNicknameChanged = viewModel::onApproverNicknameChanged,
                                onSaveNickname = viewModel::onSaveApproverNickname
                            )
                        }

                        PlanSetupUIState.AddAlternateApprover -> {
                            AddAlternateApproverUI(
                                onInviteAlternateSelected = viewModel::onInviteApprover,
                                onSaveAndFinishSelected = viewModel::onSaveAndFinishPlan
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
        val encryptedKey = state.keyData?.encryptedPrivateKey
        val participantId = state.ownerParticipantId

        if (encryptedKey != null && participantId != null) {
            CloudStorageHandler(
                actionToPerform = state.cloudStorageAction.action,
                participantId = participantId,
                encryptedPrivateKey = encryptedKey,
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