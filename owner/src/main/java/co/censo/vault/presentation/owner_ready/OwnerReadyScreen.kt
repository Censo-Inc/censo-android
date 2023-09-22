package co.censo.vault.presentation.owner_ready

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState
import co.censo.shared.presentation.components.DisplayError
import co.censo.vault.presentation.facetec_auth.FacetecAuth
import co.censo.vault.util.TestTag

@Composable
fun OwnerReadyScreen(
    ownerState: OwnerState.Ready,
    refreshOwnerState: () -> Unit,
    updateOwnerState: (OwnerState) -> Unit,
    viewModel: OwnerReadyScreenViewModel = hiltViewModel()
) {
    val state = viewModel.state

    val context = LocalContext.current as FragmentActivity

    DisposableEffect(key1 = ownerState) {
        viewModel.onNewOwnerState(ownerState)
        onDispose {}
    }

    if (state.ownerState == null) {
        LoadingIndicator()
    } else {
        when (state.lockStatus) {
            is OwnerReadyScreenState.LockStatus.Locked -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(color = Color.White),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextButton(
                        onClick = viewModel::initUnlock,
                        modifier = Modifier.semantics { testTag = TestTag.unlock_button },
                    ) {
                        Row() {
                            Icon(
                                imageVector = Icons.Rounded.LockOpen,
                                contentDescription = "Unlock",
                                tint = Color.Black
                            )
                            Text(
                                text = "Unlock",
                                color = Color.Black
                            )
                        }
                    }
                }
            }
            is OwnerReadyScreenState.LockStatus.Unlocked -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(color = Color.White),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Unlocked",
                        textAlign = TextAlign.Center,
                        color = Color.Black
                    )

                    Spacer(Modifier.height(24.dp))

                    LockCountDown(state.lockStatus.locksIn, onTimeOut = refreshOwnerState)

                    Spacer(Modifier.height(24.dp))

                    TextButton(
                        onClick = { viewModel.initLock(updateOwnerState) },
                        modifier = Modifier.semantics { testTag = TestTag.unlock_button },
                    ) {
                        Row() {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Lock",
                                tint = Color.Black
                            )
                            Text(
                                text = "Lock",
                                color = Color.Black
                            )
                        }
                    }
                }
            }
            is OwnerReadyScreenState.LockStatus.UnlockInProgress -> {
                when (state.lockStatus.apiCall) {
                    is Resource.Uninitialized -> {
                        FacetecAuth(
                            onFaceScanReady = { verificationId, facetecData ->
                                viewModel.onFaceScanReady(verificationId, facetecData, updateOwnerState)
                            }
                        )
                    }
                    is Resource.Error -> {
                        DisplayError(
                            errorMessage = state.lockStatus.apiCall.getErrorMessage(context),
                            dismissAction = null,
                            retryAction = viewModel::initUnlock
                        )
                    }
                    else -> {
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
                }
            }
            is OwnerReadyScreenState.LockStatus.LockInProgress -> {
                when (state.lockStatus.apiCall) {
                    is Resource.Error -> {
                        DisplayError(
                            errorMessage = state.lockStatus.apiCall.getErrorMessage(context),
                            dismissAction = null,
                            retryAction = viewModel::initUnlock
                        )
                    }
                    else -> {
                        LoadingIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
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
