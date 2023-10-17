package co.censo.vault.presentation.initial_plan_setup

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.shared.SharedScreen
import co.censo.shared.data.Resource
import co.censo.shared.presentation.components.DisplayError
import co.censo.vault.R
import co.censo.vault.presentation.VaultColors
import co.censo.vault.presentation.facetec_auth.FacetecAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialPlanSetupScreen(
    navController: NavController,
    viewModel: InitialPlanSetupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state = viewModel.state

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose {}
    }

    LaunchedEffect(key1 = state) {
        if (state.complete) {
            navController.navigate(SharedScreen.OwnerWelcomeScreen.route)
            viewModel.resetComplete()
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
            when (val setupStatus = state.initialPlanSetupStatus) {
                is InitialPlanSetupScreenState.InitialPlanSetupStatus.None -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = VaultColors.PrimaryColor,
                        strokeWidth = 3.dp,
                    )
                }

                is InitialPlanSetupScreenState.InitialPlanSetupStatus.ApproverKeyCreationFailed -> {
                    DisplayError(
                        errorMessage = "Error Occurred trying to save your approver key",
                        dismissAction = null,
                        retryAction = {
                            viewModel.createApproverKey()
                        }
                    )
                }

                is InitialPlanSetupScreenState.InitialPlanSetupStatus.Initial ->
                    InitialPlanSetupStandardUI {
                        viewModel.startPolicySetup()
                    }


                is InitialPlanSetupScreenState.InitialPlanSetupStatus.SetupInProgress ->
                    when (val resourceStatus = setupStatus.apiCall) {
                        is Resource.Uninitialized ->
                            FacetecAuth(
                                onFaceScanReady = viewModel::onPolicySetupCreationFaceScanReady,
                                onCancelled = {
                                    navController.navigate(SharedScreen.OwnerWelcomeScreen.route)
                                    viewModel.resetComplete()
                                }
                            )

                        is Resource.Loading ->
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = VaultColors.PrimaryColor,
                                strokeWidth = 3.dp,
                            )

                        is Resource.Error ->
                            DisplayError(
                                errorMessage = resourceStatus.getErrorMessage(context),
                                dismissAction = null,
                                retryAction = {
                                    viewModel.reset()
                                }
                            )

                        else -> {}
                    }

                is InitialPlanSetupScreenState.InitialPlanSetupStatus.CreateInProgress ->
                    when (val resourceStatus = setupStatus.apiCall) {
                        is Resource.Uninitialized,
                        is Resource.Loading ->
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = VaultColors.PrimaryColor,
                                strokeWidth = 3.dp,
                            )

                        is Resource.Error ->
                            DisplayError(
                                errorMessage = resourceStatus.getErrorMessage(context),
                                dismissAction = null,
                                retryAction = {
                                    viewModel.reset()
                                }
                            )

                        else -> {}
                    }
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
            .background(color = Color.White),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Image(
            painter = painterResource(id = co.censo.shared.R.drawable.large_face_scan),
            contentDescription = null,
            modifier = Modifier.padding(all = 20.dp),
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
        )
        Spacer(modifier = Modifier.padding(PaddingValues(horizontal = 0.dp, vertical = 30.dp)))
        Text(
            text = stringResource(R.string.step_1),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(PaddingValues(horizontal = 30.dp, vertical = 1.dp)),
        )
        Text(
            text = stringResource(R.string.scan_your_face),
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            modifier = Modifier.padding(PaddingValues(horizontal = 30.dp, vertical = 1.dp))
        )
        Spacer(modifier = Modifier.padding(PaddingValues(horizontal = 0.dp, vertical = 12.dp)))
        Text(
            text = stringResource(R.string.scan_phrase_1),
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            modifier = Modifier.padding(PaddingValues(horizontal = 30.dp, vertical = 1.dp))
        )
        Text(
            text = stringResource(R.string.scan_phrase_2),
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            modifier = Modifier.padding(PaddingValues(horizontal = 30.dp, vertical = 20.dp))
        )

        Button(
            onClick = startPlanSetup,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            ),
            modifier = Modifier.padding(32.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(
                    modifier = Modifier.padding(
                        PaddingValues(
                            horizontal = 15.dp,
                            vertical = 0.dp
                        )
                    )
                )
                Image(
                    painter = painterResource(id = co.censo.shared.R.drawable.small_face_scan_white),
                    contentDescription = null,
                    modifier = Modifier.width(32.dp)
                )
                Spacer(
                    modifier = Modifier.padding(
                        PaddingValues(
                            horizontal = 10.dp,
                            vertical = 0.dp
                        )
                    )
                )
                Text(
                    text = stringResource(R.string.begin_face_scan),
                    fontWeight = FontWeight.Medium,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(all = 8.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.White),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.info),
                    modifier = Modifier
                        .height(26.dp)
                        .padding(6.dp)
                )
                Text(stringResource(id = R.string.info))
            }
        }
    }
}

@Preview()
@Composable
fun InitialPlanSetupStandardUIPreview() {
    InitialPlanSetupStandardUI {}
}