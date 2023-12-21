package co.censo.censo.presentation.owner_key_recovery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.censo.R
import co.censo.censo.presentation.facetec_auth.FacetecAuth
import co.censo.shared.data.Resource
import co.censo.shared.data.storage.CloudStoragePermissionNotGrantedException
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.presentation.cloud_storage.CloudStorageHandler
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.LargeLoading
import kotlinx.coroutines.delay
import co.censo.censo.presentation.owner_key_recovery.components.Recovered
import co.censo.censo.presentation.plan_finalization.ReplacePolicyAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerKeyRecoveryScreen(
    navController: NavController,
    viewModel: OwnerKeyRecoveryViewModel = hiltViewModel()
) {
    val state = viewModel.state

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
                viewModel.onCreate()
            }
            else -> Unit
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            when {
                state.loading -> LargeLoading(fullscreen = true)

                state.asyncError -> {
                    if (state.verifyApproverKeysSignature is Resource.Error) {
                        DisplayError(
                            errorMessage = stringResource(R.string.cannot_verify_confirmation_signature),
                            dismissAction = {
                                viewModel.resetVerifyApproverKeysSignature()
                                viewModel.receiveAction(KeyRecoveryAction.Retry)
                            },
                            retryAction = {
                                viewModel.resetVerifyApproverKeysSignature()
                                viewModel.receiveAction(KeyRecoveryAction.Retry)
                            },
                        )
                    } else if (state.retrieveAccessShardsResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to retrieve recovery data, try again.",
                            dismissAction = {
                                viewModel.resetRetrieveAccessShardsResponse()
                                viewModel.receiveAction(KeyRecoveryAction.Retry)
                            },
                            retryAction = {
                                viewModel.resetRetrieveAccessShardsResponse()
                                viewModel.receiveAction(KeyRecoveryAction.Retry)
                            },
                        )
                    } else if (state.replaceShardsResponse is Resource.Error) {
                        if (state.replaceShardsResponse.exception is CloudStoragePermissionNotGrantedException) {
                            DisplayError(
                                errorMessage = "Google Drive Access Required for Censo\n\nPlease sign out and sign back in to refresh authentication permissions for your account",
                                dismissAction = {
                                    viewModel.dismissCloudError()
                                },
                                retryAction = null
                            )
                        } else {
                            DisplayError(
                                errorMessage = "Failed to recover key, try again.",
                                dismissAction = {
                                    viewModel.resetReplaceShardsResponse()
                                    viewModel.receiveAction(KeyRecoveryAction.Retry)
                                },
                                retryAction = {
                                    viewModel.resetReplaceShardsResponse()
                                    viewModel.receiveAction(KeyRecoveryAction.Retry)
                                },
                            )
                        }
                    } else if (state.saveKeyToCloud is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to setup secure data, try again.",
                            dismissAction = { viewModel.receiveAction(KeyRecoveryAction.Retry) },
                            retryAction = { viewModel.receiveAction(KeyRecoveryAction.Retry) },
                        )
                    } else {
                        DisplayError(
                            errorMessage = "Something went wrong, please try again.",
                            dismissAction = { viewModel.receiveAction(KeyRecoveryAction.Retry) },
                            retryAction = { viewModel.receiveAction(KeyRecoveryAction.Retry) },
                        )
                    }
                }

                else -> {
                    when (state.ownerKeyUIState) {
                        OwnerKeyRecoveryUIState.Initial -> LargeLoading(
                            fullscreen = true
                        )

                        OwnerKeyRecoveryUIState.AccessInProgress -> {
                            FacetecAuth(
                                onFaceScanReady = { verificationId, biometry ->
                                    viewModel.onFaceScanReady(verificationId, biometry)
                                },
                                onCancelled = {
                                    viewModel.receiveAction(KeyRecoveryAction.BackClicked)
                                }
                            )
                        }

                        OwnerKeyRecoveryUIState.Completed -> {
                            Recovered()
                            LaunchedEffect(Unit) {
                                delay(6000)
                                viewModel.receiveAction(KeyRecoveryAction.Completed)
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
            val participantId = state.ownerParticipantId

            if (encryptedKey != null && participantId != null) {
                CloudStorageHandler(
                    actionToPerform = state.cloudStorageAction.action,
                    participantId = participantId,
                    encryptedPrivateKey = encryptedKey,
                    onActionSuccess = {
                        viewModel.receiveAction(KeyRecoveryAction.KeyUploadSuccess)
                    },
                    onActionFailed = {
                        viewModel.receiveAction(KeyRecoveryAction.KeyUploadFailed(it))
                    }
                )
            } else {
                val exceptionCause =
                    if (encryptedKey == null) "missing private key" else "missing participant id"
                viewModel.receiveAction(KeyRecoveryAction.KeyUploadFailed(
                    Exception("Unable to setup policy $exceptionCause")))
            }
        }
    }
}