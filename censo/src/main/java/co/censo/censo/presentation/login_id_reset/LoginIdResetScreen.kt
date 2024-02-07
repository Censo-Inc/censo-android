package co.censo.censo.presentation.login_id_reset

import android.annotation.SuppressLint
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.censo.R
import co.censo.censo.presentation.VaultColors
import co.censo.censo.presentation.components.TermsOfUse
import co.censo.censo.presentation.facetec_auth.FacetecAuth
import co.censo.censo.presentation.initial_plan_setup.ScanFaceInformationUI
import co.censo.censo.presentation.login_id_reset.components.LoginIdResetUI
import co.censo.censo.presentation.login_id_reset.components.PasswordInputUI
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GoogleAuthError
import co.censo.shared.data.model.touVersion
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.LargeLoading
import co.censo.shared.util.ClipboardHelper
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.sendError
import com.google.android.gms.auth.api.signin.GoogleSignIn

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun LoginIdResetScreen(
    resetToken: String?,
    navController: NavController,
    viewModel: LoginIdResetViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val state = viewModel.state

    val showInfoView: MutableState<Boolean> = remember { mutableStateOf(false) }

    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { activityResult ->
            when (activityResult.resultCode) {
                RESULT_OK -> {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(activityResult.data)
                    viewModel.handleSignInResult(task)
                }

                RESULT_CANCELED -> {
                    viewModel.googleAuthCancel()
                }

                else -> {
                    viewModel.googleAuthFailure(GoogleAuthError.IntentResultFailed)
                }
            }
        }
    )

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart(resetToken)
        onDispose { }
    }

    LaunchedEffect(key1 = state) {
        if (state.triggerGoogleSignIn is Resource.Success) {
            try {
                val googleSignInClient = viewModel.getGoogleSignInClient()

                val intent = googleSignInClient.signInIntent
                googleAuthLauncher.launch(intent)
            } catch (e: Exception) {
                e.sendError(CrashReportingUtil.LoginIdReset)
                viewModel.googleAuthFailure(GoogleAuthError.FailedToLaunchGoogleAuthUI(e))
            }
            viewModel.resetTriggerGoogleSignIn()
        }

        if (state.navigationResource is Resource.Success) {
            state.navigationResource.data.let { destination ->
                navController.navigate(destination) {
                    popUpTo(destination) {
                        inclusive = true
                    }
                }
            }
            viewModel.resetNavigationResource()
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = VaultColors.NavbarColor),
            navigationIcon = {
                if (state.resetStep != LoginIdResetStep.TermsOfUse) {
                    IconButton(onClick = {
                        if (showInfoView.value) {
                            showInfoView.value = false
                        } else {
                            viewModel.receiveAction(LoginIdResetAction.Exit)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            stringResource(R.string.exit),
                            tint = SharedColors.MainIconColor
                        )
                    }
                }
            },
            title = {},
        )
    }) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            when {
                state.isLoading -> LargeLoading(
                    fullscreen = true,
                    fullscreenBackgroundColor = Color.White
                )

                state.apiCallErrorOccurred -> {
                    if (state.triggerGoogleSignIn is Resource.Error) {
                        DisplayError(
                            errorMessage = state.triggerGoogleSignIn.getErrorMessage(context),
                            dismissAction = viewModel::resetTriggerGoogleSignIn,
                            retryAction = { viewModel.receiveAction(LoginIdResetAction.Retry) },
                        )
                    } else if (state.createDeviceResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = state.createDeviceResponse.getErrorMessage(context),
                            dismissAction = viewModel::resetCreateDeviceResponse,
                            retryAction = { viewModel.receiveAction(LoginIdResetAction.Retry) },
                        )
                    } else if (state.resetLoginIdResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = state.resetLoginIdResponse.getErrorMessage(context),
                            dismissAction = viewModel::resetResetLoginIdResponse,
                            retryAction = { viewModel.receiveAction(LoginIdResetAction.Retry) },
                        )
                    } else if (state.userResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = state.userResponse.getErrorMessage(context),
                            dismissAction = null,
                            retryAction = { viewModel.receiveAction(LoginIdResetAction.Retry) },
                        )
                    } else if (state.authTypeResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = state.authTypeResponse.getErrorMessage(context),
                            dismissAction = viewModel::resetAuthTypeResponse,
                            retryAction = null,
                        )
                    }
                }

                state.linkError -> {
                    DisplayError(
                        errorMessage = "Link is not valid - (${
                            ClipboardHelper.getClipboardContent(
                                context
                            )
                        })",
                        dismissAction = { viewModel.clearLinkError() },
                        retryAction = null
                    )
                }

                else -> {
                    when (state.resetStep) {
                        LoginIdResetStep.Password -> {
                            PasswordInputUI(
                                onPasswordInputFinished = { viewModel.receiveAction(LoginIdResetAction.PasswordInputFinished(it)) }
                            )
                        }

                        LoginIdResetStep.FacetecBiometryConsent -> {
                            ScanFaceInformationUI(
                                startPlanSetup = { viewModel.receiveAction(LoginIdResetAction.StartFacescan) },
                                isInfoViewVisible = showInfoView.value,
                                showInfoView = {
                                    showInfoView.value = true
                                }
                            )
                        }

                        LoginIdResetStep.TermsOfUse -> {
                            TermsOfUse(
                                onAccept = { viewModel.receiveAction(LoginIdResetAction.TermsOfUseAccepted(touVersion)) },
                                onCancel = { viewModel.receiveAction(LoginIdResetAction.Exit) },
                                onboarding = false
                            )
                        }

                        else -> {
                            LoginIdResetUI(
                                resetStep = state.resetStep,
                                linksCollected = state.collectedTokens,
                                linksRequired = state.requiredTokens,
                                onPasteLink = {
                                    viewModel.receiveAction(
                                        LoginIdResetAction.PasteLink(
                                            ClipboardHelper.getClipboardContent(context)
                                        )
                                    )
                                },
                                onSelectGoogleId = { viewModel.receiveAction(LoginIdResetAction.SelectGoogleId) },
                                onContinueToBiometry = { viewModel.receiveAction(LoginIdResetAction.RetrieveAuthType) },
                                onKeyRecovery = { viewModel.receiveAction(LoginIdResetAction.KeyRecovery) }
                            )
                        }
                    }
                }
            }

            if (state.launchFacetec) {
                FacetecAuth(
                    onFaceScanReady = { verificationId, biometry ->
                        viewModel.onFaceScanReady(
                            verificationId,
                            biometry
                        )
                    }
                )
            }
        }
    }
}
