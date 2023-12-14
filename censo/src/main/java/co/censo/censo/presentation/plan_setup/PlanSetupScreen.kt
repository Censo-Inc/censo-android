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
import co.censo.censo.presentation.plan_setup.components.Activated
import co.censo.censo.presentation.plan_setup.components.ApproversRemoved
import co.censo.shared.data.model.ApproverStatus
import co.censo.shared.data.storage.CloudStoragePermissionNotGrantedException
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.presentation.cloud_storage.CloudStorageHandler
import co.censo.shared.presentation.components.LargeLoading
import co.censo.shared.util.LinksUtil
import co.censo.shared.util.projectLog
import kotlinx.coroutines.delay

enum class PlanSetupDirection(val threshold: UInt) {
    AddApprovers(2U), RemoveApprovers(1U)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanSetupScreen(
    navController: NavController,
    planSetupDirection: PlanSetupDirection,
    setupViewModel: PlanSetupViewModel = hiltViewModel(),
    finalizationViewModel: PlanFinalizationViewModel = hiltViewModel()
) {
    val setupState = setupViewModel.state
    val finalizationState = finalizationViewModel.state//TODO: Propagate this across the screen

    fun displaySetupUI() =
        finalizationState.planFinalizationUIState == PlanFinalizationUIState.Uninitialized_0
                && setupState.planSetupUIState != PlanSetupUIState.Uninitialized_0

    val iconPair = when (setupState.backArrowType) {
        PlanSetupState.BackIconType.Back -> Icons.Filled.ArrowBack to R.string.back
        PlanSetupState.BackIconType.Exit -> Icons.Filled.Clear to R.string.exit
        else -> null
    }

    LaunchedEffect(key1 = setupState) {
        if (setupState.navigationResource is Resource.Success) {
            setupState.navigationResource.data?.let {
                navController.navigate(it)
                setupViewModel.resetNavigationResource()
            }
        }

        if (setupState.finalizePlanSetup is Resource.Success) {
            setupViewModel.resetFinalizePlanSetup()
            finalizationViewModel.onFinalizePlanSetup(setupState.planSetupDirection)
        }
    }

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                setupViewModel.onCreate(planSetupDirection)
            }
            Lifecycle.Event.ON_RESUME -> {
                setupViewModel.onResume()
            }
            Lifecycle.Event.ON_PAUSE -> {
                setupViewModel.onStop()
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
                                setupViewModel.receivePlanAction(PlanSetupAction.BackClicked)
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
                    when (setupState.planSetupUIState) {
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
                setupState.loading || finalizationState.loading -> LargeLoading(fullscreen = true)

                setupState.asyncError || finalizationState.asyncError -> {
                    //TODO: Diligently walk through these states and update them to reference the correct area
                    if (setupState.verifyKeyConfirmationSignature is Resource.Error) {
                        DisplayError(
                            errorMessage = stringResource(R.string.cannot_verify_confirmation_signature),
                            dismissAction = {
                                setupViewModel.resetVerifyKeyConfirmationSignature()
                                setupViewModel.receivePlanAction(PlanSetupAction.Retry)
                            },
                            retryAction = {
                                setupViewModel.resetVerifyKeyConfirmationSignature()
                                setupViewModel.receivePlanAction(PlanSetupAction.Retry)
                            },
                        )
                    } else if (setupState.userResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to retrieve user information, try again.",
                            dismissAction = {
                                setupViewModel.resetUserResponse()
                                setupViewModel.receivePlanAction(PlanSetupAction.Retry)
                            },
                            retryAction = {
                                setupViewModel.resetUserResponse()
                                setupViewModel.receivePlanAction(PlanSetupAction.Retry)
                            },
                        )
                    } else if (setupState.createPolicySetupResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to create policy, try again",
                            dismissAction = {
                                setupViewModel.resetCreatePolicySetupResponse()
                                setupViewModel.receivePlanAction(PlanSetupAction.Retry)
                            },
                            retryAction = {
                                setupViewModel.resetCreatePolicySetupResponse()
                                setupViewModel.receivePlanAction(PlanSetupAction.Retry)
                            },
                        )
                    } else if (setupState.initiateAccessResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to replace plan, try again.",
                            dismissAction = {
                                setupViewModel.resetInitiateAccessResponse()
                                setupViewModel.receivePlanAction(PlanSetupAction.Retry)
                            },
                            retryAction = {
                                setupViewModel.resetInitiateAccessResponse()
                                setupViewModel.receivePlanAction(PlanSetupAction.Retry)
                            },
                        )
                    } else if (setupState.retrieveAccessShardsResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to retrieve recovery data, try again.",
                            dismissAction = {
                                setupViewModel.resetRetrieveAccessShardsResponse()
                                setupViewModel.receivePlanAction(PlanSetupAction.Retry)
                            },
                            retryAction = {
                                setupViewModel.resetRetrieveAccessShardsResponse()
                                setupViewModel.receivePlanAction(PlanSetupAction.Retry)
                            },
                        )
                    } else if (finalizationState.replacePolicyResponse is Resource.Error) {
                        if (finalizationState.replacePolicyResponse.exception is CloudStoragePermissionNotGrantedException) {
                            DisplayError(
                                errorMessage = "Google Drive Access Required for Censo\n\nPlease sign out and sign back in to refresh authentication permissions for your account",
                                dismissAction = {
                                    finalizationViewModel.dismissCloudError()
                                },
                                retryAction = null
                            )
                        } else {
                            DisplayError(
                                errorMessage = "Failed to create new policy, try again.",
                                dismissAction = {
                                    finalizationViewModel.resetReplacePolicyResponse()
                                    finalizationViewModel.receivePlanAction(PlanFinalizationAction.Retry)
                                },
                                retryAction = {
                                    finalizationViewModel.resetReplacePolicyResponse()
                                    finalizationViewModel.receivePlanAction(PlanFinalizationAction.Retry)
                                },
                            )
                        }
                    } else if (setupState.completeApprovershipResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to finalize plan, try again.",
                            dismissAction = { setupViewModel.receivePlanAction(PlanSetupAction.Retry) },
                            retryAction = { setupViewModel.receivePlanAction(PlanSetupAction.Retry) },
                        )
                    } else if (finalizationState.saveKeyToCloud is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to setup secure data, try again.",
                            dismissAction = { finalizationViewModel.receivePlanAction(PlanFinalizationAction.Retry) },
                            retryAction = { finalizationViewModel.receivePlanAction(PlanFinalizationAction.Retry) },
                        )
                    } else {
                        DisplayError(
                            errorMessage = "Something went wrong, please try again.",
                            dismissAction = { setupViewModel.receivePlanAction(PlanSetupAction.Retry) },
                            retryAction = { setupViewModel.receivePlanAction(PlanSetupAction.Retry) },
                        )
                    }
                }

                else -> {

                    if (displaySetupUI()) {
                        when (setupState.planSetupUIState) {
                            PlanSetupUIState.Initial_1 -> LargeLoading(
                                fullscreen = true
                            )

                            PlanSetupUIState.ApproverNickname_2 -> {
                                ApproverNicknameUI(
                                    isFirstApprover = setupState.primaryApprover?.status !is ApproverStatus.Confirmed,
                                    nickname = setupState.editedNickname,
                                    enabled = setupState.editedNicknameValid,
                                    nicknameIsTooLong = setupState.editedNicknameIsTooLong,
                                    onNicknameChanged = {
                                        setupViewModel.receivePlanAction(PlanSetupAction.ApproverNicknameChanged(it))
                                    },
                                    onSaveNickname = {
                                        setupViewModel.receivePlanAction(PlanSetupAction.SaveApproverAndSavePolicy)
                                    }
                                )
                            }

                            PlanSetupUIState.EditApproverNickname_3 -> {
                                ApproverNicknameUI(
                                    isFirstApprover = setupState.primaryApprover?.status !is ApproverStatus.Confirmed,
                                    isRename = true,
                                    nickname = setupState.editedNickname,
                                    enabled = setupState.editedNicknameValid,
                                    nicknameIsTooLong = setupState.editedNicknameIsTooLong,
                                    onNicknameChanged = {
                                        setupViewModel.receivePlanAction(PlanSetupAction.ApproverNicknameChanged(it))
                                    },
                                    onSaveNickname = {
                                        setupViewModel.receivePlanAction(PlanSetupAction.EditApproverAndSavePolicy)
                                    }
                                )
                            }

                            //Really light screen. Just moves us to next UI or let's user back out.
                            PlanSetupUIState.ApproverGettingLive_4 -> {
                                GetLiveWithUserUI(
                                    title = "${stringResource(R.string.activate_approver)} ${setupState.editedNickname}",
                                    message = stringResource(R.string.activate_your_approver_message, setupState.editedNickname),
                                    activatingApprover = true,
                                    onContinueLive = {
                                        setupViewModel.receivePlanAction(PlanSetupAction.GoLiveWithApprover)
                                    },
                                    onResumeLater = {
                                        setupViewModel.receivePlanAction(PlanSetupAction.BackClicked)
                                    }
                                )
                            }

                            //Verify approver while approver does full onboarding
                            PlanSetupUIState.ApproverActivation_5 -> {
                                ActivateApproverUI(
                                    prospectApprover = setupState.activatingApprover,
                                    secondsLeft = setupState.secondsLeft,
                                    verificationCode = setupState.approverCodes[setupState.activatingApprover?.participantId] ?: "",
                                    storesLink = LinksUtil.CENSO_APPROVER_STORE_LINK,
                                    onContinue = {
                                        setupViewModel.receivePlanAction(PlanSetupAction.ApproverConfirmed)
                                    },
                                    onEditNickname = {
                                        setupViewModel.receivePlanAction(PlanSetupAction.EditApproverNickname)
                                    }
                                )
                            }

                            else -> {}
                        }
                    } else {
                        when (finalizationState.planFinalizationUIState) {
                            //TODO: May need loading UI
                            PlanFinalizationUIState.AccessInProgress_1 -> {
                                FacetecAuth(
                                    onFaceScanReady = { verificationId, biometry ->
                                        finalizationViewModel.onFaceScanReady(verificationId, biometry)
                                    },
                                    onCancelled = {
                                        setupViewModel.receivePlanAction(PlanSetupAction.BackClicked)
                                    }
                                )
                            }

                            PlanFinalizationUIState.Completed_2 -> {
                                when (planSetupDirection) {
                                    PlanSetupDirection.AddApprovers -> Activated()
                                    PlanSetupDirection.RemoveApprovers -> ApproversRemoved()
                                }

                                LaunchedEffect(Unit) {
                                    delay(6000)
                                    //TODO: Diligently check where this should go in the data after this method
                                    finalizationViewModel.receivePlanAction(PlanFinalizationAction.Completed)
                                }
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    }

    if (finalizationState.cloudStorageAction.triggerAction) {
        if (finalizationState.cloudStorageAction.action == CloudStorageActions.UPLOAD) {
            val encryptedKey = finalizationState.keyData?.encryptedPrivateKey
            val participantId = finalizationState.ownerApprover?.participantId

            if (encryptedKey != null && participantId != null) {
                CloudStorageHandler(
                    actionToPerform = finalizationState.cloudStorageAction.action,
                    participantId = participantId,
                    encryptedPrivateKey = encryptedKey,
                    onActionSuccess = {
                        finalizationViewModel.receivePlanAction(PlanFinalizationAction.KeyUploadSuccess)
                    },
                    onActionFailed = {
                        finalizationViewModel.receivePlanAction(PlanFinalizationAction.KeyUploadFailed(it))
                    }
                )
            } else {
                val exceptionCause =
                    if (encryptedKey == null) "missing private key" else "missing participant id"
                finalizationViewModel.receivePlanAction(PlanFinalizationAction.KeyUploadFailed(
                    Exception("Unable to setup policy $exceptionCause")))
            }
        } else if (finalizationState.cloudStorageAction.action == CloudStorageActions.DOWNLOAD) {
            val participantId = setupState.ownerApprover?.participantId

            if (participantId != null) {
                CloudStorageHandler(
                    actionToPerform = finalizationState.cloudStorageAction.action,
                    participantId = participantId,
                    encryptedPrivateKey = null,
                    onActionSuccess = {
                        finalizationViewModel.receivePlanAction(PlanFinalizationAction.KeyDownloadSuccess(it))
                    },
                    onActionFailed = {
                        finalizationViewModel.receivePlanAction(PlanFinalizationAction.KeyDownloadFailed(it))
                    }
                )
            } else {
                finalizationViewModel.receivePlanAction(
                    PlanFinalizationAction.KeyDownloadFailed(
                        Exception("Unable to setup policy, missing participant id")
                    )
                )
            }
        }
    }
}