package co.censo.censo.presentation.initial_plan_setup

import StandardButton
import TitleText
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.presentation.cloud_storage.CloudStorageHandler
import co.censo.shared.presentation.components.DisplayError
import co.censo.censo.R
import co.censo.censo.presentation.Screen
import co.censo.censo.presentation.VaultColors
import co.censo.censo.presentation.facetec_auth.FacetecAuth
import co.censo.shared.presentation.ButtonTextStyle
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.presentation.components.LargeLoading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialPlanSetupScreen(
    navController: NavController,
    viewModel: InitialPlanSetupViewModel = hiltViewModel()
) {
    val state = viewModel.state

    LaunchedEffect(key1 = state) {
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


            navController.navigate(route)
            viewModel.reset()
        }
    }


    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = VaultColors.NavbarColor),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            stringResource(R.string.back),
                            tint = SharedColors.MainIconColor
                        )
                    }
                },
                title = {},
            )
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
                        InitialPlanSetupStep.PolicyCreation -> LargeLoading(fullscreen = true)

                        is InitialPlanSetupStep.Initial ->
                            InitialPlanSetupStandardUI {
                                viewModel.determineUIStatus()
                            }


                        is InitialPlanSetupStep.Facetec ->
                            FacetecAuth(
                                onFaceScanReady = viewModel::onPolicyCreationFaceScanReady,
                                onCancelled = {
                                    viewModel.reset()
                                }
                            )
                    }
                }
            }

            if (state.cloudStorageAction.triggerAction) {
                val privateKey = state.keyData?.encryptedPrivateKey

                CloudStorageHandler(
                    actionToPerform = state.cloudStorageAction.action,
                    participantId = state.participantId,
                    encryptedPrivateKey = privateKey,
                    onActionSuccess = { encryptedByteArray ->
                        when (state.cloudStorageAction.action) {
                            CloudStorageActions.UPLOAD -> viewModel.onKeySaved()
                            CloudStorageActions.DOWNLOAD -> viewModel.onKeyLoaded(encryptedByteArray)
                            else -> {}
                        }
                    },
                    onActionFailed = {
                        when (state.cloudStorageAction.action) {
                            CloudStorageActions.UPLOAD -> viewModel.onKeySaveFailed(exception = it)
                            CloudStorageActions.DOWNLOAD -> viewModel.onKeyLoadFailed(exception = it)
                            else -> {}
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun InitialPlanSetupStandardUI(
    startPlanSetup: () -> Unit,
) {

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
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
            Text(
                text = stringResource(R.string.affirmative_biometric_consent),
                fontWeight = FontWeight.Light,
                fontStyle = FontStyle.Italic,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                color = SharedColors.MainColorText,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(screenHeight * 0.025f))
            StandardButton(
                onClick = startPlanSetup,
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(id = co.censo.shared.R.drawable.small_face_scan_white),
                        contentDescription = null,
                        modifier = Modifier.width(32.dp),
                        colorFilter = ColorFilter.tint(SharedColors.ButtonTextBlue)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.begin_face_scan),
                        style = ButtonTextStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.Medium),
                    )
                }
            }

            Spacer(modifier = Modifier.height(screenHeight * 0.05f))
        }
    }
}

@Preview(device = Devices.PIXEL_4_XL, showBackground = true, showSystemUi = true)
@Composable
fun LargeInitialPlanSetupStandardUIPreview() {
    InitialPlanSetupStandardUI {}
}

@Preview(device = Devices.PIXEL_4, showBackground = true, showSystemUi = true)
@Composable
fun MediumInitialPlanSetupStandardUIPreview() {
    InitialPlanSetupStandardUI {}
}

@Preview(device = Devices.NEXUS_5, showBackground = true, showSystemUi = true)
@Composable
fun SmallInitialPlanSetupStandardUIPreview() {
    InitialPlanSetupStandardUI {}
}