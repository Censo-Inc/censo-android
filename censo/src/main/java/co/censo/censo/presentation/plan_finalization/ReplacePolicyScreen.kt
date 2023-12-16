package co.censo.censo.presentation.plan_finalization

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.censo.R
import co.censo.censo.presentation.Screen
import co.censo.censo.presentation.facetec_auth.FacetecAuth
import co.censo.censo.presentation.plan_setup.PolicySetupAction
import co.censo.censo.presentation.plan_setup.PolicySetupScreenAction
import co.censo.censo.presentation.plan_setup.components.Activated
import co.censo.censo.presentation.plan_setup.components.ApproversRemoved
import co.censo.shared.data.Resource
import co.censo.shared.data.storage.CloudStoragePermissionNotGrantedException
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.presentation.cloud_storage.CloudStorageHandler
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.LargeLoading
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplacePolicyScreen(
    navController: NavController,
    policySetupAction: PolicySetupAction,
    viewModel: ReplacePolicyViewModel = hiltViewModel()
) {
    val state = viewModel.state

    val iconPair = when (state.backArrowType) {
        ReplacePolicyState.BackIconType.EXIT -> Icons.Filled.Clear to R.string.exit
        else -> null
    }

    LaunchedEffect(key1 = state) {
        if (state.navigationResource is Resource.Success) {
            state.navigationResource.data?.let {
                navController.navigate(it) {
                    val popUpToRoute = if (state.policySetupAction == PolicySetupAction.AddApprovers) {
                        Screen.ReplacePolicyRoute.addApproversRoute()
                    } else {
                        Screen.ReplacePolicyRoute.removeApproversRoute()
                    }

                    popUpTo(popUpToRoute) {
                        inclusive = true
                    }
                }
                viewModel.resetNavigationResource()
            }
        }
    }

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                viewModel.onCreate(policySetupAction)
            }

            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    when (iconPair) {
                        null -> {}
                        else -> {
                            IconButton(
                                onClick = {
                                    viewModel.receivePlanAction(ReplacePolicyAction.BackClicked)
                                }) {
                                Icon(
                                    imageVector = iconPair.first,
                                    contentDescription = stringResource(id = iconPair.second),
                                )
                            }
                        }
                    }
                },
                title = { })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            when {
                state.loading -> LargeLoading(fullscreen = true)

                state.asyncError -> {
                    if (state.verifyKeyConfirmationSignature is Resource.Error) {
                        DisplayError(
                            errorMessage = stringResource(R.string.cannot_verify_confirmation_signature),
                            dismissAction = {
                                viewModel.resetVerifyKeyConfirmationSignature()
                                viewModel.receivePlanAction(ReplacePolicyAction.Retry)
                            },
                            retryAction = {
                                viewModel.resetVerifyKeyConfirmationSignature()
                                viewModel.receivePlanAction(ReplacePolicyAction.Retry)
                            },
                        )
                    } else if (state.userResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to retrieve user information, try again.",
                            dismissAction = {
                                viewModel.resetUserResponse()
                                viewModel.receivePlanAction(ReplacePolicyAction.Retry)
                            },
                            retryAction = {
                                viewModel.resetUserResponse()
                                viewModel.receivePlanAction(ReplacePolicyAction.Retry)
                            },
                        )
                    } else if (state.createPolicySetupResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to create policy, try again",
                            dismissAction = {
                                viewModel.resetCreatePolicySetupResponse()
                                viewModel.receivePlanAction(ReplacePolicyAction.Retry)
                            },
                            retryAction = {
                                viewModel.resetCreatePolicySetupResponse()
                                viewModel.receivePlanAction(ReplacePolicyAction.Retry)
                            },
                        )
                    } else if (state.initiateAccessResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to replace plan, try again.",
                            dismissAction = {
                                viewModel.resetInitiateAccessResponse()
                                viewModel.receivePlanAction(ReplacePolicyAction.Retry)
                            },
                            retryAction = {
                                viewModel.resetInitiateAccessResponse()
                                viewModel.receivePlanAction(ReplacePolicyAction.Retry)
                            },
                        )
                    } else if (state.retrieveAccessShardsResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to retrieve recovery data, try again.",
                            dismissAction = {
                                viewModel.resetRetrieveAccessShardsResponse()
                                viewModel.receivePlanAction(ReplacePolicyAction.Retry)
                            },
                            retryAction = {
                                viewModel.resetRetrieveAccessShardsResponse()
                                viewModel.receivePlanAction(ReplacePolicyAction.Retry)
                            },
                        )
                    } else if (state.completeApprovershipResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to finalize plan, try again.",
                            dismissAction = { viewModel.receivePlanAction(ReplacePolicyAction.Retry) },
                            retryAction = { viewModel.receivePlanAction(ReplacePolicyAction.Retry) },
                        )
                    } else if (state.saveKeyToCloud is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to setup secure data, try again.",
                            dismissAction = {
                                viewModel.receivePlanAction(
                                    ReplacePolicyAction.Retry
                                )
                            },
                            retryAction = {
                                viewModel.receivePlanAction(
                                    ReplacePolicyAction.Retry
                                )
                            },
                        )
                    } else if (state.replacePolicyResponse is Resource.Error) {
                        if (state.replacePolicyResponse.exception is CloudStoragePermissionNotGrantedException) {
                            DisplayError(
                                errorMessage = "Google Drive Access Required for Censo\n\nPlease go to settings to enable Google Drive permissions for your account",
                                dismissAction = {
                                    viewModel.dismissCloudError()
                                },
                                retryAction = null
                            )
                        } else {
                            DisplayError(
                                errorMessage = "Failed to create new policy, try again.",
                                dismissAction = {
                                    viewModel.resetReplacePolicyResponse()
                                    viewModel.receivePlanAction(ReplacePolicyAction.Retry)
                                },
                                retryAction = {
                                    viewModel.resetReplacePolicyResponse()
                                    viewModel.receivePlanAction(ReplacePolicyAction.Retry)
                                },
                            )
                        }
                    } else {
                        DisplayError(
                            errorMessage = "Something went wrong, please try again.",
                            dismissAction = { viewModel.receivePlanAction(ReplacePolicyAction.Retry) },
                            retryAction = { viewModel.receivePlanAction(ReplacePolicyAction.Retry) },
                        )
                    }
                }

                else -> {
                    when (state.replacePolicyUIState) {
                        ReplacePolicyUIState.Uninitialized_1 -> LargeLoading(fullscreen = true)

                        ReplacePolicyUIState.AccessInProgress_2 -> {
                            FacetecAuth(
                                onFaceScanReady = { verificationId, biometry ->
                                    viewModel.onFaceScanReady(verificationId, biometry)
                                },
                                onCancelled = {
                                    viewModel.receivePlanAction(ReplacePolicyAction.FacetecCancelled)
                                }
                            )
                        }

                        ReplacePolicyUIState.Completed_3 -> {
                            when (policySetupAction) {
                                PolicySetupAction.AddApprovers -> Activated()
                                PolicySetupAction.RemoveApprovers -> ApproversRemoved()
                            }

                            LaunchedEffect(Unit) {
                                delay(6000)
                                viewModel.receivePlanAction(ReplacePolicyAction.Completed)
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
                        viewModel.receivePlanAction(ReplacePolicyAction.KeyUploadSuccess)
                    },
                    onActionFailed = {
                        viewModel.receivePlanAction(
                            ReplacePolicyAction.KeyUploadFailed(
                                it
                            )
                        )
                    }
                )
            } else {
                val exceptionCause =
                    if (encryptedKey == null) "missing private key" else "missing participant id"
                viewModel.receivePlanAction(
                    ReplacePolicyAction.KeyUploadFailed(
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
                            ReplacePolicyAction.KeyDownloadSuccess(
                                it
                            )
                        )
                    },
                    onActionFailed = {
                        viewModel.receivePlanAction(
                            ReplacePolicyAction.KeyDownloadFailed(
                                it
                            )
                        )
                    }
                )
            } else {
                viewModel.receivePlanAction(
                    ReplacePolicyAction.KeyDownloadFailed(
                        Exception("Unable to setup policy, missing participant id")
                    )
                )
            }
        }
    }
}