package co.censo.vault.presentation.plan_setup

import FullScreenButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIos
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.presentation.Colors
import co.censo.shared.presentation.components.DisplayError
import co.censo.vault.R
import co.censo.vault.presentation.components.security_plan.AddApproverDialog
import co.censo.vault.presentation.components.security_plan.EditOrDeleteMenu
import co.censo.vault.presentation.components.security_plan.InitialAddApproverScreen
import co.censo.vault.presentation.components.security_plan.RequiredApprovalsScreen
import co.censo.vault.presentation.components.security_plan.ReviewPlanScreen
import co.censo.vault.presentation.components.security_plan.SecureYourPlanScreen
import co.censo.vault.presentation.components.security_plan.SelectApproversScreen
import co.censo.vault.presentation.components.security_plan.SetupSecurityPlanScreen
import co.censo.vault.presentation.facetec_auth.FacetecAuth
import co.censo.vault.presentation.home.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanSetupScreen(
    navController: NavController,
    viewModel: PlanSetupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state = viewModel.state

    val bottomButtonText = when (state.currentScreen) {
        SetupSecurityPlanScreen.Initial -> stringResource(R.string.select_first_approver_title)
        SetupSecurityPlanScreen.AddApprovers -> stringResource(R.string.next_required_approvals)
        SetupSecurityPlanScreen.RequiredApprovals -> stringResource(R.string.next_review)
        SetupSecurityPlanScreen.Review -> stringResource(R.string.confirm)
        SetupSecurityPlanScreen.SecureYourPlan -> stringResource(id = R.string.continue_text)
        SetupSecurityPlanScreen.FacetecAuth -> ""
    }

    LaunchedEffect(key1 = state) {
        if (state.navigateToActivateApprovers) {
            navController.navigate(Screen.ActivateApprovers.route)
            viewModel.resetNavToActivateApprovers()
        }
    }

    Scaffold(
        contentColor = Color.White,
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Colors.PrimaryBlue),
                navigationIcon = {
                    if (state.showBackIcon) {
                        IconButton(onClick = viewModel::onBackActionClick) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBackIos,
                                stringResource(R.string.back),
                                tint = Color.White
                            )
                        }

                    } else {
                        Spacer(modifier = Modifier)
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.setup_security_plan),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                },
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .background(color = Color.White),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                if (state.mainButtonCount > 1) {
                    FullScreenButton(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = Color.White,
                        textColor = Colors.PrimaryBlue,
                        border = true,
                        contentPadding = PaddingValues(vertical = 6.dp),
                        onClick = viewModel::onMoreInfoClicked
                    ) {
                        Text(
                            text = stringResource(R.string.how_does_this_work),
                            color = Colors.PrimaryBlue,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.W300
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (state.mainButtonCount > 0) {

                    FullScreenButton(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = Colors.PrimaryBlue,
                        textColor = Color.White,
                        border = false,
                        contentPadding = PaddingValues(vertical = 12.dp),
                        onClick = viewModel::onMainActionClick,
                    )
                    {
                        Text(
                            text = bottomButtonText,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.W700
                        )
                    }
                }
            }
        }
    ) { paddingValues ->

        when (state.currentScreen) {
            SetupSecurityPlanScreen.Initial ->
                InitialAddApproverScreen(paddingValues = paddingValues)

            SetupSecurityPlanScreen.AddApprovers ->
                SelectApproversScreen(
                    paddingValues = paddingValues,
                    guardians = state.guardians,
                    addApproverOnClick = viewModel::showAddGuardianDialog,
                    editApproverOnClick = viewModel::showEditOrDeleteDialog
                )

            SetupSecurityPlanScreen.RequiredApprovals ->
                RequiredApprovalsScreen(
                    paddingValues = paddingValues,
                    guardians = state.guardians,
                    sliderPosition = state.threshold.toFloat(),
                    updateThreshold = viewModel::updateSliderPosition
                )

            SetupSecurityPlanScreen.Review ->
                ReviewPlanScreen(
                    paddingValues = paddingValues,
                    guardians = state.guardians,
                    sliderPosition = state.threshold.toFloat(),
                    updateThreshold = viewModel::updateSliderPosition,
                    editApprover = viewModel::showEditOrDeleteDialog,
                    addApprover = viewModel::showAddGuardianDialog
                )

            SetupSecurityPlanScreen.SecureYourPlan ->
                SecureYourPlanScreen(paddingValues = paddingValues)

            SetupSecurityPlanScreen.FacetecAuth ->
                FacetecAuth(
                    onFaceScanReady = viewModel::onPolicySetupCreationFaceScanReady
                )
        }

        if (state.showAddGuardianDialog) {
            AddApproverDialog(
                paddingValues = paddingValues,
                nickname = state.addedApproverNickname,
                onDismiss = viewModel::dismissDialog,
                updateApproverName = viewModel::updateAddedApproverNickname,
                submit = viewModel::submitNewApprover
            )
        }

        if (state.showEditOrDeleteDialog) {
            EditOrDeleteMenu(
                onDismiss = viewModel::dismissDialog,
                edit = viewModel::editGuardian,
                delete = viewModel::deleteGuardian
            )
        }

        if (state.asyncError) {
            if (state.createPolicySetupResponse is Resource.Error) {
                DisplayError(
                    errorMessage = state.createPolicySetupResponse.getErrorMessage(context),
                    dismissAction = {
                        viewModel.resetCreatePolicySetup()
                    },
                    retryAction = {
                        viewModel.retryFacetec()
                    }
                )
            }
        }
    }
}