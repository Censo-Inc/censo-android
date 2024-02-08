package co.censo.censo.presentation.initial_plan_setup

import TitleText
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.presentation.components.DisplayError
import co.censo.censo.R
import co.censo.censo.presentation.Screen
import co.censo.censo.presentation.components.BeginFaceScanButton
import co.censo.censo.presentation.facetec_auth.FacetecAuth
import co.censo.censo.presentation.onboarding.OnboardingTopBar
import co.censo.shared.util.popCurrentDestinationFromBackStack
import co.censo.shared.presentation.components.ConfirmationDialog
import co.censo.shared.presentation.components.LargeLoading
import co.censo.shared.presentation.components.LearnMoreScreen
import co.censo.shared.presentation.components.LearnMoreUI
import co.censo.shared.presentation.components.LearnMoreUtil
import co.censo.shared.util.popUpToTop

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun InitialPlanSetupScreen(
    navController: NavController,
    viewModel: InitialPlanSetupViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val showInfoView: MutableState<Boolean> = remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler(
        enabled = state.showPromoCodeUI,
        onBack = viewModel::dismissPromoCodeUI
    )

    LaunchedEffect(key1 = state) {
        if (state.kickUserOut is Resource.Success) {
            navController.navigate(Screen.EntranceRoute.route) {
                launchSingleTop = true
                popUpToTop()
            }
            viewModel.delayedReset()
        }

        if (state.complete) {

            val publicKey =
                (state.createPolicyParamsResponse as? Resource.Success)?.data?.masterEncryptionPublicKey
                    ?: state.createPolicyParams?.masterEncryptionPublicKey

            val route = publicKey?.let {
                Screen.EnterPhraseRoute.buildNavRoute(
                    masterPublicKey = it,
                    welcomeFlow = true
                )
            } ?: Screen.EntranceRoute.route

            navController.navigate(route) {
                popCurrentDestinationFromBackStack(navController)
            }
            viewModel.delayedReset()
        }
    }


    Scaffold(
        containerColor = Color.White,
        topBar = {
            if (state.initialPlanSetupStep == InitialPlanSetupStep.Initial) {
                OnboardingTopBar(
                    onCancel = {
                        if (showInfoView.value) {
                            showInfoView.value = false
                        } else if (state.welcomeStep == WelcomeStep.ScanningFace) {
                          viewModel.changeWelcomeStep(WelcomeStep.Authenticated)
                        } else {
                            viewModel.showDeleteUserDialog()
                        }
                    },
                    onboarding = true
                )
            }
        },

        ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            when {
                state.apiError -> {
                    val errorText =
                        if (state.saveKeyToCloudResource is Resource.Error) {
                            "Error occurred while trying to save data to Google Drive. Please try again."
                        } else if (state.loadKeyFromCloudResource is Resource.Error) {
                            "Error occurred while trying to load data from Google Drive. Please try again."
                        } else if (state.deleteUserResource is Resource.Error) {
                            "Error occurred while trying to reset user data"
                        } else if (state.createPolicyParamsResponse is Resource.Error
                            || state.createPolicyResponse is Resource.Error
                        ) {
                            "Error occurred while trying to create data. Please try again."
                        } else {
                            "Failed to complete facetec, try again."
                        }

                    DisplayError(
                        errorMessage = errorText,
                        dismissAction = null,
                        retryAction = {
                            viewModel.resetError()
                            viewModel.determineUIStatus()
                        }
                    )
                }

                else -> {

                    when (state.initialPlanSetupStep) {
                        InitialPlanSetupStep.CreateApproverKey,
                        InitialPlanSetupStep.CreatePolicyParams,
                        InitialPlanSetupStep.PolicyCreation,
                        InitialPlanSetupStep.DeleteUser -> LargeLoading(fullscreen = true)

                        is InitialPlanSetupStep.Initial -> {
                            if (state.welcomeStep == WelcomeStep.Authenticated) {
                                WelcomeScreenUI(
                                    isPromoCodeEnabled = !state.promoCodeAccepted,
                                    showPromoCodeUI = viewModel::showPromoCodeUI,
                                    onMainButtonClick = {
                                        viewModel.changeWelcomeStep(WelcomeStep.ScanningFace)
                                    },
                                    onMinorButtonClick = {
                                        navController.navigate(Screen.AcceptBeneficiaryInvitation.buildNavRoute(null))
                                    }
                                )
                            } else {
                                ScanFaceInformationUI(
                                    beginFaceScan = viewModel::determineUIStatus,
                                    isInfoViewVisible = showInfoView.value,
                                    showInfoView = {
                                        showInfoView.value = true
                                    }
                                )
                            }

                            if (state.triggerDeleteUserDialog is Resource.Success) {
                                ConfirmationDialog(
                                    title = stringResource(id = R.string.exit_setup),
                                    message = stringResource(R.string.exit_setup_details),
                                    onCancel = viewModel::resetDeleteUserDialog,
                                    onDelete = viewModel::deleteUser,
                                )
                            }
                        }


                        is InitialPlanSetupStep.Facetec ->
                            FacetecAuth(
                                onFaceScanReady = viewModel::onPolicyCreationFaceScanReady,
                                onCancelled = {
                                    viewModel.delayedReset()
                                }
                            )
                    }
                }
            }
        }
    }

    //Sits on top of entire screen to cover nav bar
    if (state.initialPlanSetupStep is InitialPlanSetupStep.Initial && state.welcomeStep == WelcomeStep.Authenticated) {
        if (state.showPromoCodeUI) {
            EnterPromoCodeUI(
                loading = state.promoCodeLoading,
                inputtedPromoCode = state.promoCode,
                updatePromoCode = viewModel::updatePromoCode,
                submitPromoCode = {
                    keyboardController?.hide()
                    viewModel.submitPromoCode()
                },
                dismissPromoCodeUI = viewModel::dismissPromoCodeUI
            )
        }

        if (state.promoCodeSuccessDialog) {
            PromoCodeDialog(
                title = stringResource(id = R.string.promo_code_accepted),
                message = stringResource(id = R.string.promo_code_accepted_message),
                onDismiss = viewModel::dismissPromoDialog
            )
        }

        if (state.promoCodeErrorDialog) {
            PromoCodeDialog(
                title = stringResource(id = R.string.error),
                message = stringResource(R.string.unknown_promo_code),
                onDismiss = viewModel::dismissPromoDialog
            )
        }
    }
}

@Composable
fun ScanFaceInformationUI(
    beginFaceScan: () -> Unit,
    isInfoViewVisible: Boolean,
    showInfoView: () -> Unit
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(color = Color.White),
        verticalArrangement = Arrangement.Bottom
    ) {

        Box(modifier = Modifier
            .fillMaxWidth()
            .weight(0.1f)
        ) {
            Image(
                modifier = Modifier
                    .padding(top = screenHeight * 0.015f)
                    .align(Alignment.Center),
                painter = painterResource(id = R.drawable.face_scan_hand_with_phone),
                contentDescription = null,
                contentScale = ContentScale.Fit
            )
        }

        Column(modifier = Modifier.padding(horizontal = 44.dp)) {
            TitleText(
                title = R.string.scan_your_face,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(screenHeight * 0.025f))
            BeginFaceScanButton(
                spacing = screenHeight * 0.025f,
                onBeginFaceScan = beginFaceScan
            )
            Spacer(modifier = Modifier.height(screenHeight * 0.025f))
            LearnMoreUI {
                showInfoView()
            }

            Spacer(modifier = Modifier.height(screenHeight * 0.025f))
        }
    }

    if (isInfoViewVisible) {
        LearnMoreScreen(
            title = stringResource(R.string.face_scan_learn_more_title),
            annotatedString = LearnMoreUtil.faceScanMessage(),
        )
    }
}

@Composable
fun PromoCodeDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        title = {
            Text(
                text = title,
                color = Color.Black,
                fontSize = 24.sp
            )
        },
        text = {
            Text(
                text = message,
                color = Color.Black,
                fontSize = 18.sp
            )
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            ClickableText(
                text = buildAnnotatedString { append(stringResource(id = R.string.ok)) },
                style = TextStyle(
                    color = Color.Black,
                    fontSize = 20.sp
                ),
                onClick = { onDismiss() }
            )
        },
    )
}

@Preview(device = Devices.PIXEL_4_XL, showBackground = true, showSystemUi = true)
@Composable
fun LargeInitialPlanSetupStandardUIPreview() {
    ScanFaceInformationUI(
        isInfoViewVisible = false,
        beginFaceScan = {},
        showInfoView = {}
    )
}

@Preview(device = Devices.PIXEL_4, showBackground = true, showSystemUi = true)
@Composable
fun MediumInitialPlanSetupStandardUIPreview() {
    ScanFaceInformationUI(
        isInfoViewVisible = false,
        beginFaceScan = {},
        showInfoView = {}
    )
}

@Preview(device = Devices.NEXUS_5, showBackground = true, showSystemUi = true)
@Composable
fun SmallInitialPlanSetupStandardUIPreview() {
    ScanFaceInformationUI(
        isInfoViewVisible = false,
        beginFaceScan = {},
        showInfoView = {}
    )
}
