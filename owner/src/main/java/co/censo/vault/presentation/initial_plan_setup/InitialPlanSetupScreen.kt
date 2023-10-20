package co.censo.vault.presentation.initial_plan_setup

import LearnMore
import StandardButton
import SubTitleText
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val state = viewModel.state

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

            when {
                state.apiError -> {
                    if (state.saveKeyToCloudResource is Resource.Error) {
                        DisplayError(
                            errorMessage = "Error occurred trying to save your approver key",
                            dismissAction = null,
                            retryAction = {
                                viewModel.createApproverKey()
                            }
                        )
                    } else if (state.createPolicyParams is Resource.Error) {
                        DisplayError(
                            errorMessage = "Error occurred trying to create policy params",
                            dismissAction = null,
                            retryAction = {
                                viewModel.createPolicyParams()
                            }
                        )
                    } else if (state.createPolicyResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Error occurred trying to create policy",
                            dismissAction = null,
                            retryAction = {
                                viewModel.startFacetec()
                            }
                        )
                    }
                }

                else -> {

                    when (state.initialPlanSetupStep) {
                        InitialPlanSetupStep.CreateApproverKey,
                        InitialPlanSetupStep.CreatePolicyParams,
                        InitialPlanSetupStep.PolicyCreation ->
                            Box(modifier = Modifier.fillMaxSize()) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(72.dp).align(Alignment.Center),
                                    color = VaultColors.PrimaryColor,
                                    strokeWidth = 8.dp,
                                )
                            }

                        is InitialPlanSetupStep.Initial ->
                            InitialPlanSetupStandardUI {
                                viewModel.moveToNextAction()
                            }


                        is InitialPlanSetupStep.Facetec ->
                            FacetecAuth(
                                onFaceScanReady = viewModel::onPolicyCreationFaceScanReady,
                                onCancelled = {
                                    navController.navigate(SharedScreen.OwnerWelcomeScreen.route)
                                    viewModel.resetComplete()
                                }
                            )
                    }
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
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val horizontalPadding = 32.dp
        Image(
            painter = painterResource(id = co.censo.shared.R.drawable.large_face_scan),
            contentScale = ContentScale.FillWidth,
            contentDescription = null,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
        )
        Spacer(modifier = Modifier.padding(PaddingValues(vertical = 30.dp)))
        SubTitleText(
            subtitle = R.string.step_1,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .padding(PaddingValues(horizontal = horizontalPadding))
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.padding(PaddingValues(vertical = 2.dp)))
        TitleText(
            title = R.string.scan_your_face,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .padding(PaddingValues(horizontal = horizontalPadding))
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(PaddingValues(horizontal = 0.dp, vertical = 12.dp)))
        Text(
            text = stringResource(R.string.scan_phrase),
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            modifier = Modifier
                .padding(PaddingValues(horizontal = horizontalPadding))
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        StandardButton(
            onClick = startPlanSetup,
            color = Color.Black,
            contentPadding = PaddingValues(vertical = 12.dp),
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
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

        Spacer(modifier = Modifier.height(24.dp))

        LearnMore {

        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview()
@Composable
fun InitialPlanSetupStandardUIPreview() {
    InitialPlanSetupStandardUI {}
}