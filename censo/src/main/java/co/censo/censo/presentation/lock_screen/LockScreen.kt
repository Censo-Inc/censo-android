package co.censo.censo.presentation.lock_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.censo.presentation.facetec_auth.FacetecAuth
import co.censo.censo.presentation.lock_screen.components.LockEngagedUI
import co.censo.shared.data.Resource
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.LargeLoading

@Composable
fun LockedScreen(
    navController: NavController,
    viewModel: LockScreenViewModel = hiltViewModel(),
) {
    val state = viewModel.state

    val context = LocalContext.current as FragmentActivity

    val interactionSource = remember { MutableInteractionSource() }

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                viewModel.onCreate()
            }

            Lifecycle.Event.ON_RESUME -> {
                viewModel.onStart()
            }

            Lifecycle.Event.ON_PAUSE -> {
                viewModel.onStop()
            }

            else -> Unit
        }
    }

    LaunchedEffect(key1 = state) {
        if (state.navigationResource is Resource.Success) {
            state.navigationResource.data.let { navigationData ->
                navController.navigate(navigationData.route)
            }
            viewModel.resetNavigationResource()
        }
    }

    Box(modifier = Modifier
        .clickable(
            indication = null,
            interactionSource = interactionSource
        ) { }
    ) {
        when (val lockStatus = state.lockStatus) {
            is LockScreenState.LockStatus.Locked -> {
                LockEngagedUI(
                    initUnlock = viewModel::initUnlock,
                    canRequestBiometryReset = lockStatus.canRequestBiometryReset,
                    initResetBiometry = { viewModel.navigateToResetBiometry(lockStatus) },
                )
                if (lockStatus.biometryResetRequested) {
                    LargeLoading(
                        fullscreen = true,
                        fullscreenBackgroundColor = Color.White
                    )
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
                            modifier = Modifier.background(color = Color.White),
                            errorMessage = lockStatus.apiCall.getErrorMessage(context),
                            dismissAction = null,
                            retryAction = viewModel::initUnlock
                        )
                    }

                    else -> LargeLoading(
                        color = SharedColors.DefaultLoadingColor,
                        fullscreen = true,
                        fullscreenBackgroundColor = Color.White
                    )
                }
            }

            is LockScreenState.LockStatus.Unlocked,
            is LockScreenState.LockStatus.None -> {
                Spacer(modifier = Modifier.height(0.dp))
            }
        }
    }
}