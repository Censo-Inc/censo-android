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
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.presentation.cloud_storage.CloudStorageHandler
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.util.projectLog
import co.censo.censo.R
import co.censo.censo.presentation.Screen
import co.censo.censo.presentation.VaultColors
import co.censo.censo.presentation.facetec_auth.FacetecAuth
import co.censo.shared.presentation.components.Loading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialPlanSetupScreen(
    navController: NavController,
    viewModel: InitialPlanSetupViewModel = hiltViewModel()
) {
    val state = viewModel.state

    LaunchedEffect(key1 = state) {
        if (state.complete) {
            navController.navigate(Screen.OwnerWelcomeScreen.route)
            viewModel.reset()
        }
    }


    Scaffold(
        contentColor = Color.Black,
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = VaultColors.NavbarColor),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            stringResource(R.string.back),
                            tint = Color.Black
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
                            "Error occurred trying to save your approver key"
                        } else if (state.createPolicyParamsResponse is Resource.Error) {
                            "Error occurred trying to create policy params"
                        } else if (state.createPolicyResponse is Resource.Error) {
                            "Error occurred trying to create policy"
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
                        InitialPlanSetupStep.PolicyCreation -> Loading(
                            strokeWidth = 8.dp,
                            color = VaultColors.PrimaryColor,
                            size = 72.dp,
                            fullscreen = true
                        )

                        is InitialPlanSetupStep.Initial ->
                            InitialPlanSetupStandardUI {
                                viewModel.determineUIStatus()
                            }


                        is InitialPlanSetupStep.Facetec ->
                            FacetecAuth(
                                onFaceScanReady = viewModel::onPolicyCreationFaceScanReady,
                                onCancelled = {
                                    navController.navigate(Screen.OwnerWelcomeScreen.route)
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
                    onActionSuccess = { _ ->
                        projectLog(message = "Cloud Storage action success")
                        viewModel.onKeySaved()
                    },
                    onActionFailed = {
                        projectLog(message = "Cloud Storage action failed")
                        viewModel.onKeySaveFailed(exception = it)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(color = Color.White),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.padding(32.dp)) {
            Image(
                painter = painterResource(id = co.censo.shared.R.drawable.large_face_scan),
                contentScale = ContentScale.FillWidth,
                contentDescription = null,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
            )
        }

        Column(modifier = Modifier.padding(horizontal = 44.dp)) {
            TitleText(
                title = R.string.scan_your_face,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.scan_phrase_message),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.affirmative_biometric_consent),
                fontWeight = FontWeight.Light,
                fontStyle = FontStyle.Italic,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            StandardButton(
                onClick = startPlanSetup,
                color = Color.Black,
                contentPadding = PaddingValues(vertical = 20.dp),
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(id = co.censo.shared.R.drawable.small_face_scan_white),
                        contentDescription = null,
                        modifier = Modifier.width(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.begin_face_scan),
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 24.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(44.dp))
        }
    }
}

@Preview()
@Composable
fun InitialPlanSetupStandardUIPreview() {
    InitialPlanSetupStandardUI {}
}