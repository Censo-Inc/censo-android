package co.censo.vault.presentation.lock_screen

import FullScreenButton
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import co.censo.shared.data.Resource
import co.censo.shared.presentation.components.DisplayError
import co.censo.vault.R
import co.censo.vault.presentation.VaultColors
import co.censo.vault.presentation.facetec_auth.FacetecAuth

@Composable
fun LockedScreen(
    unlockedModifier: Modifier,
    viewModel: LockScreenViewModel = hiltViewModel(),
) {
    val state = viewModel.state

    val context = LocalContext.current as FragmentActivity

    val interactionSource = remember { MutableInteractionSource() }

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose {}
    }

    Box(modifier = Modifier
        .clickable(
            indication = null,
            interactionSource = interactionSource
        ) { }
    ) {
        when (val lockStatus = state.lockStatus) {
            is LockScreenState.LockStatus.Locked -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(color = VaultColors.PrimaryColor),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,

                    ) {

                    Text(
                        text = stringResource(R.string.vault_is_locked),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.W400
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    FullScreenButton(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = VaultColors.PrimaryColor,
                        borderColor = Color.White,
                        border = true,
                        contentPadding = PaddingValues(vertical = 12.dp),
                        onClick = viewModel::initUnlock,
                    ) {
                        Text(
                            text = stringResource(R.string.unlock),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.W700
                        )
                    }
                }
            }

            is LockScreenState.LockStatus.Unlocked -> {
                Column(
                    unlockedModifier.background(color = VaultColors.PrimaryColor),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Spacer(modifier = Modifier.weight(1f))

                    LockCountDown(
                        lockStatus.locksAt,
                        onTimeOut = viewModel::retrieveOwnerState
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FullScreenButton(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = VaultColors.PrimaryColor,
                        borderColor = Color.White,
                        border = true,
                        contentPadding = PaddingValues(vertical = 12.dp),
                        onClick = viewModel::initLock
                    ) {
                        Text(
                            text = stringResource(R.string.lock),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.W700
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            is LockScreenState.LockStatus.UnlockInProgress -> {
                when (lockStatus.apiCall) {
                    is Resource.Uninitialized -> {
                        FacetecAuth(
                            onFaceScanReady = { verificationId, facetecData ->
                                viewModel.onFaceScanReady(
                                    verificationId,
                                    facetecData
                                )
                            },
                            onCancelled = {
                                viewModel.resetToLocked()
                            }
                        )
                    }

                    is Resource.Error -> {
                        DisplayError(
                            modifier = Modifier.background(color = VaultColors.PrimaryColor),
                            errorMessage = lockStatus.apiCall.getErrorMessage(context),
                            dismissAction = null,
                            retryAction = viewModel::initUnlock
                        )
                    }

                    else -> {
                        LoadingIndicator()
                    }
                }
            }

            is LockScreenState.LockStatus.LockInProgress -> {
                when (lockStatus.apiCall) {
                    is Resource.Error -> {
                        DisplayError(
                            modifier = Modifier.background(color = VaultColors.PrimaryColor),
                            errorMessage = lockStatus.apiCall.getErrorMessage(context),
                            dismissAction = null,
                            retryAction = viewModel::initUnlock
                        )
                    }

                    else -> {
                        LoadingIndicator()
                    }
                }
            }

            is LockScreenState.LockStatus.None -> {
                Spacer(modifier = Modifier.height(0.dp))
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = VaultColors.PrimaryColor),
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.Center),
            strokeWidth = 8.dp,
            color = Color.White
        )
    }
}
