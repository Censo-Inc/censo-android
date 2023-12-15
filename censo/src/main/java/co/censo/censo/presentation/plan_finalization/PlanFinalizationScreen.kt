package co.censo.censo.presentation.plan_finalization

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
import co.censo.censo.R
import co.censo.censo.presentation.facetec_auth.FacetecAuth
import co.censo.censo.presentation.plan_setup.PlanSetupDirection
import co.censo.censo.presentation.plan_setup.components.Activated
import co.censo.censo.presentation.plan_setup.components.ApproversRemoved
import co.censo.shared.data.Resource
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.presentation.cloud_storage.CloudStorageHandler
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.LargeLoading
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanFinalizationScreen(
    navController: NavController,
    planSetupDirection: PlanSetupDirection,
    viewModel: PlanFinalizationViewModel = hiltViewModel()
) {
    val state = viewModel.state

    //TODO: Flesh out this screen with necessary UI
//    val iconPair = when (state.backArrowType) {
//        PlanSetupState.BackIconType.Back -> Icons.Filled.ArrowBack to R.string.back
//        PlanSetupState.BackIconType.Exit -> Icons.Filled.Clear to R.string.exit
//        else -> null
//    }

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
            Lifecycle.Event.ON_CREATE -> {
                viewModel.onCreate(planSetupDirection)
            }

            else -> Unit
        }
    }

    Scaffold(topBar = {
//        TopAppBar(
//            navigationIcon = {
//                when (iconPair) {
//                    null -> {}
//                    else -> {
//                        IconButton(
//                            onClick = {
//                                viewModel.receivePlanAction(PlanSetupAction.BackClicked)
//                            }) {
//                            Icon(
//                                imageVector = iconPair.first,
//                                contentDescription = stringResource(id = iconPair.second),
//                            )
//                        }
//                    }
//                }
//            },
//            title = {
//                Text(
//                    text =
//                    when (state.planSetupUIState) {
//                        PlanSetupUIState.ApproverActivation_5,
//                        PlanSetupUIState.EditApproverNickname_3 ->
//                            stringResource(R.string.add_approver_title)
//
//                        else -> ""
//                    }
//                )
//            }
//        )
    }) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            when {
                state.loading -> LargeLoading(fullscreen = true)

                state.asyncError -> {
                    //TODO: Diligently walk through these states and update them to reference the correct area
                    if (state.verifyKeyConfirmationSignature is Resource.Error) {
                        DisplayError(
                            errorMessage = stringResource(R.string.cannot_verify_confirmation_signature),
                            dismissAction = {
                                viewModel.resetVerifyKeyConfirmationSignature()
                                viewModel.receivePlanAction(PlanFinalizationAction.Retry)
                            },
                            retryAction = {
                                viewModel.resetVerifyKeyConfirmationSignature()
                                viewModel.receivePlanAction(PlanFinalizationAction.Retry)
                            },
                        )
                    } else if (state.userResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to retrieve user information, try again.",
                            dismissAction = {
                                viewModel.resetUserResponse()
                                viewModel.receivePlanAction(PlanFinalizationAction.Retry)
                            },
                            retryAction = {
                                viewModel.resetUserResponse()
                                viewModel.receivePlanAction(PlanFinalizationAction.Retry)
                            },
                        )
                    } else if (state.createPolicySetupResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to create policy, try again",
                            dismissAction = {
                                viewModel.resetCreatePolicySetupResponse()
                                viewModel.receivePlanAction(PlanFinalizationAction.Retry)
                            },
                            retryAction = {
                                viewModel.resetCreatePolicySetupResponse()
                                viewModel.receivePlanAction(PlanFinalizationAction.Retry)
                            },
                        )
                    } else if (state.initiateAccessResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to replace plan, try again.",
                            dismissAction = {
                                viewModel.resetInitiateAccessResponse()
                                viewModel.receivePlanAction(PlanFinalizationAction.Retry)
                            },
                            retryAction = {
                                viewModel.resetInitiateAccessResponse()
                                viewModel.receivePlanAction(PlanFinalizationAction.Retry)
                            },
                        )
                    } else if (state.retrieveAccessShardsResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to retrieve recovery data, try again.",
                            dismissAction = {
                                viewModel.resetRetrieveAccessShardsResponse()
                                viewModel.receivePlanAction(PlanFinalizationAction.Retry)
                            },
                            retryAction = {
                                viewModel.resetRetrieveAccessShardsResponse()
                                viewModel.receivePlanAction(PlanFinalizationAction.Retry)
                            },
                        )
                    } else if (state.completeApprovershipResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to finalize plan, try again.",
                            dismissAction = { viewModel.receivePlanAction(PlanFinalizationAction.Retry) },
                            retryAction = { viewModel.receivePlanAction(PlanFinalizationAction.Retry) },
                        )
                    } else if (state.saveKeyToCloud is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to setup secure data, try again.",
                            dismissAction = { viewModel.receivePlanAction(
                                PlanFinalizationAction.Retry) },
                            retryAction = { viewModel.receivePlanAction(
                                PlanFinalizationAction.Retry) },
                        )
                    } else {
                        DisplayError(
                            errorMessage = "Something went wrong, please try again.",
                            dismissAction = { viewModel.receivePlanAction(PlanFinalizationAction.Retry) },
                            retryAction = { viewModel.receivePlanAction(PlanFinalizationAction.Retry) },
                        )
                    }
                }

                else -> {
                        when (state.planFinalizationUIState) {
                            //TODO: May need loading UI
                            PlanFinalizationUIState.AccessInProgress_1 -> {
                                FacetecAuth(
                                    onFaceScanReady = { verificationId, biometry ->
                                        viewModel.onFaceScanReady(verificationId, biometry)
                                    },
                                    onCancelled = {
                                        viewModel.receivePlanAction(PlanFinalizationAction.FacetecCancelled)
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
                                    viewModel.receivePlanAction(PlanFinalizationAction.Completed)
                                }
                            }

                            else -> {}
                        }

                }
            }
        }
    }

    //TODO: Move to finalization scree
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
                        viewModel.receivePlanAction(PlanFinalizationAction.KeyUploadSuccess)
                    },
                    onActionFailed = {
                        viewModel.receivePlanAction(
                            PlanFinalizationAction.KeyUploadFailed(
                                it
                            )
                        )
                    }
                )
            } else {
                val exceptionCause =
                    if (encryptedKey == null) "missing private key" else "missing participant id"
                viewModel.receivePlanAction(
                    PlanFinalizationAction.KeyUploadFailed(
                        Exception("Unable to setup policy $exceptionCause")
                    )
                )
            }
        } else if (state.cloudStorageAction.action == CloudStorageActions.DOWNLOAD) {
            val participantId = state.ownerApprover?.participantId

            if (participantId != null) {
                CloudStorageHandler(
                    actionToPerform = state.cloudStorageAction.action,
                    participantId = participantId,
                    encryptedPrivateKey = null,
                    onActionSuccess = {
                        viewModel.receivePlanAction(
                            PlanFinalizationAction.KeyDownloadSuccess(
                                it
                            )
                        )
                    },
                    onActionFailed = {
                        viewModel.receivePlanAction(
                            PlanFinalizationAction.KeyDownloadFailed(
                                it
                            )
                        )
                    }
                )
            } else {
                viewModel.receivePlanAction(
                    PlanFinalizationAction.KeyDownloadFailed(
                        Exception("Unable to setup policy, missing participant id")
                    )
                )
            }
        }
    }
}