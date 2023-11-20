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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
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
import co.censo.censo.presentation.facetec_auth.FacetecAuth
import co.censo.censo.presentation.plan_setup.components.ActivateApproverUI
import co.censo.censo.presentation.plan_setup.components.ApproverNicknameUI
import co.censo.censo.presentation.plan_setup.components.AddAlternateApproverUI
import co.censo.censo.presentation.plan_setup.components.SavedAndShardedUI
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
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
                        IconButton(
                            onClick = {
                                viewModel.receivePlanAction(PlanSetupAction.BackClicked)
                            }) {
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
                        PlanSetupUIState.ApproverActivation_5,
                        PlanSetupUIState.EditApproverNickname_3 ->
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
                            dismissAction = { viewModel.receivePlanAction(PlanSetupAction.Retry) },
                            retryAction = { viewModel.receivePlanAction(PlanSetupAction.Retry) },
                        )
                    } else if (state.userResponse is Resource.Error){
                        DisplayError(
                            errorMessage = "Failed to retrieve user information, try again.",
                            dismissAction = { viewModel.receivePlanAction(PlanSetupAction.Retry) },
                            retryAction = { viewModel.receivePlanAction(PlanSetupAction.Retry) },
                        )
                    } else if (state.createPolicySetupResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to create policy, try again",
                            dismissAction = { viewModel.receivePlanAction(PlanSetupAction.Retry) },
                            retryAction = { viewModel.receivePlanAction(PlanSetupAction.Retry) },
                        )
                    } else if (state.initiateRecoveryResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to replace plan, try again.",
                            dismissAction = { viewModel.receivePlanAction(PlanSetupAction.Retry) },
                            retryAction = { viewModel.receivePlanAction(PlanSetupAction.Retry) },
                        )
                    } else if (state.retrieveRecoveryShardsResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to retrieve recovery data, try again.",
                            dismissAction = { viewModel.receivePlanAction(PlanSetupAction.Retry) },
                            retryAction = { viewModel.receivePlanAction(PlanSetupAction.Retry) },
                        )
                    } else if (state.replacePolicyResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to create new policy, try again.",
                            dismissAction = { viewModel.receivePlanAction(PlanSetupAction.Retry) },
                            retryAction = { viewModel.receivePlanAction(PlanSetupAction.Retry) },
                        )
                    } else if (state.completeGuardianShipResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to finalize plan, try again.",
                            dismissAction = { viewModel.receivePlanAction(PlanSetupAction.Retry) },
                            retryAction = { viewModel.receivePlanAction(PlanSetupAction.Retry) },
                        )
                    } else if (state.saveKeyToCloud is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to setup secure data, try again.",
                            dismissAction = { viewModel.receivePlanAction(PlanSetupAction.Retry) },
                            retryAction = { viewModel.receivePlanAction(PlanSetupAction.Retry) },
                        )
                    } else {
                        DisplayError(
                            errorMessage = "Something went wrong, please try again.",
                            dismissAction = { viewModel.receivePlanAction(PlanSetupAction.Retry) },
                            retryAction = { viewModel.receivePlanAction(PlanSetupAction.Retry) },
                        )
                    }
                }

                else -> {
                    when (state.planSetupUIState) {
                        PlanSetupUIState.Initial_1 -> Loading(
                            strokeWidth = 5.dp,
                            size = 72.dp,
                            fullscreen = true
                        )

                        PlanSetupUIState.ApproverNickname_2 -> {
                            ApproverNicknameUI(
                                isFirstApprover = state.primaryApprover?.status !is GuardianStatus.Confirmed,
                                nickname = state.editedNickname,
                                enabled = state.editedNicknameValid,
                                nicknameIsTooLong = state.editedNicknameIsTooLong,
                                onNicknameChanged = {
                                    viewModel.receivePlanAction(PlanSetupAction.ApproverNicknameChanged(it))
                                },
                                onSaveNickname = {
                                    viewModel.receivePlanAction(PlanSetupAction.SaveApproverAndSavePolicy)
                                }
                            )
                        }

                        PlanSetupUIState.EditApproverNickname_3 -> {
                            ApproverNicknameUI(
                                isFirstApprover = state.primaryApprover?.status !is GuardianStatus.Confirmed,
                                isRename = true,
                                nickname = state.editedNickname,
                                enabled = state.editedNicknameValid,
                                nicknameIsTooLong = state.editedNicknameIsTooLong,
                                onNicknameChanged = {
                                    viewModel.receivePlanAction(PlanSetupAction.ApproverNicknameChanged(it))
                                },
                                onSaveNickname = {
                                    viewModel.receivePlanAction(PlanSetupAction.EditApproverAndSavePolicy)
                                }
                            )
                        }

                        //Really light screen. Just moves us to next UI or let's user back out.
                        PlanSetupUIState.ApproverGettingLive_4 -> {
                            GetLiveWithUserUI(
                                title = "${stringResource(R.string.activate_approver)} ${state.editedNickname}",
                                message = stringResource(R.string.activate_your_approver_message, state.editedNickname),
                                activatingApprover = true,
                                onContinueLive = {
                                    viewModel.receivePlanAction(PlanSetupAction.GoLiveWithApprover)
                                },
                                onResumeLater = {
                                    viewModel.receivePlanAction(PlanSetupAction.BackClicked)
                                }
                            )
                        }

                        //Verify approver while approver does full onboarding
                        PlanSetupUIState.ApproverActivation_5 -> {
                            ActivateApproverUI(
                                prospectApprover = state.activatingApprover,
                                secondsLeft = state.secondsLeft,
                                verificationCode = state.approverCodes[state.activatingApprover?.participantId] ?: "",
                                storesLink = LinksUtil.CENSO_APPROVER_STORE_LINK,
                                onContinue = {
                                    viewModel.receivePlanAction(PlanSetupAction.ApproverConfirmed)
                                },
                                onEditNickname = {
                                    viewModel.receivePlanAction(PlanSetupAction.EditApproverNickname)
                                }
                            )
                        }


                        //Send us to Approver Getting Live or Approver Nickname.
                        //Save plan will send us to cloud flow to save key with entropy.
                        PlanSetupUIState.AddAlternateApprover_6 -> {
                            AddAlternateApproverUI(
                                onInviteAlternateSelected = {
                                    viewModel.receivePlanAction(PlanSetupAction.InviteApprover)
                                },
                                onSaveAndFinishSelected = {
                                    viewModel.receivePlanAction(PlanSetupAction.SavePlan)
                                }
                            )
                        }

                        PlanSetupUIState.RecoveryInProgress_7 -> {
                            FacetecAuth(
                                onFaceScanReady = { verificationId, biometry ->
                                    viewModel.onFaceScanReady(verificationId, biometry)
                                },
                                onCancelled = {
                                    viewModel.receivePlanAction(PlanSetupAction.BackClicked)
                                }
                            )
                        }

                        PlanSetupUIState.Completed_8 -> {
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
                                viewModel.receivePlanAction(PlanSetupAction.Completed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.cloudStorageAction.triggerAction) {
        if (state.cloudStorageAction.action == CloudStorageActions.UPLOAD) {
            val encryptedKey = state.keyData?.encryptedPrivateKey
            val participantId = state.ownerApprover?.participantId

            if (encryptedKey != null && participantId != null) {
                CloudStorageHandler(
                    actionToPerform = state.cloudStorageAction.action,
                    participantId = participantId,
                    encryptedPrivateKey = encryptedKey,
                    onActionSuccess = {
                        viewModel.receivePlanAction(PlanSetupAction.KeyUploadSuccess)
                    },
                    onActionFailed = {
                        viewModel.receivePlanAction(PlanSetupAction.KeyUploadFailed(it))
                    }
                )
            } else {
                val exceptionCause =
                    if (encryptedKey == null) "missing private key" else "missing participant id"
                viewModel.receivePlanAction(PlanSetupAction.KeyUploadFailed(
                    Exception("Unable to setup policy $exceptionCause")))
            }
        } else if (state.cloudStorageAction.action == CloudStorageActions.DOWNLOAD) {
            val participantId = state.ownerApprover?.participantId

            if (participantId != null) {
                CloudStorageHandler(
                    actionToPerform = state.cloudStorageAction.action,
                    participantId = participantId,
                    encryptedPrivateKey = null,
                    onActionSuccess = {
                        viewModel.receivePlanAction(PlanSetupAction.KeyDownloadSuccess(it))
                    },
                    onActionFailed = {
                        viewModel.receivePlanAction(PlanSetupAction.KeyDownloadFailed(it))
                    }
                )
            } else {
                viewModel.receivePlanAction(
                    PlanSetupAction.KeyDownloadFailed(
                        Exception("Unable to setup policy, missing participant id")
                    )
                )
            }
        }
    }
}