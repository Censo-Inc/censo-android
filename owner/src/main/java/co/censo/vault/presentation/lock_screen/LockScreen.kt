package co.censo.vault.presentation.lock_screen

import FullScreenButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import co.censo.shared.data.Resource
import co.censo.shared.presentation.Colors
import co.censo.shared.presentation.components.DisplayError
import co.censo.vault.presentation.facetec_auth.FacetecAuth

@Composable
fun LockedScreen(
    viewModel: LockScreenViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val state = viewModel.state

    val context = LocalContext.current as FragmentActivity

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose {}
    }

    when (val lockStatus = state.lockStatus) {
        is LockScreenState.LockStatus.Locked -> {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(color = Colors.PrimaryBlue),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Text(
                    text = "Vault is locked",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.W400
                )

                Spacer(modifier = Modifier.height(16.dp))

                FullScreenButton(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = Colors.PrimaryBlue,
                    borderColor = Color.White,
                    border = true,
                    contentPadding = PaddingValues(vertical = 12.dp),
                    onClick = viewModel::initUnlock,
                ) {
                    Text(
                        text = "Unlock",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.W400
                    )
                }
            }
        }

        is LockScreenState.LockStatus.Unlocked -> {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Row(
                    Modifier.fillMaxHeight(0.85f)
                ) {
                    content()
                }

                Column(
                    Modifier
                        .fillMaxSize()
                        .background(color = Colors.PrimaryBlue),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LockCountDown(lockStatus.locksAt, onTimeOut = viewModel::retrieveOwnerState)
                    Spacer(modifier = Modifier.height(8.dp))
                    FullScreenButton(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = Colors.PrimaryBlue,
                        borderColor = Color.White,
                        border = true,
                        contentPadding = PaddingValues(vertical = 12.dp),
                        onClick = viewModel::initLock
                    ) {
                        Text(
                            text = "Lock",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.W400
                        )
                    }
                }
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
                        }
                    )
                }

                is Resource.Error -> {
                    DisplayError(
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
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Colors.PrimaryBlue),
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
