package co.censo.vault.presentation.plan_setup

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.shared.presentation.components.DisplayError
import co.censo.vault.R
import co.censo.vault.presentation.VaultColors
import co.censo.vault.presentation.Screen
import co.censo.vault.presentation.enter_phrase.BackIconType
import co.censo.vault.presentation.plan_setup.components.AddApproverNicknameUI
import co.censo.vault.presentation.plan_setup.components.AddTrustedApproversUI
import co.censo.vault.presentation.plan_setup.components.GetLiveWithApproverUI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanSetupScreen(
    navController: NavController,
    welcomeFlow: Boolean = true,
    viewModel: PlanSetupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state = viewModel.state

    val iconPair =
        if (state.backArrowType == BackIconType.BACK) Icons.Filled.ArrowBack to R.string.back
        else Icons.Filled.Clear to R.string.exit

    LaunchedEffect(key1 = state) {
        if (state.navigateToActivateApprovers) {
            navController.navigate(Screen.ActivateApprovers.route)
            viewModel.resetNavToActivateApprovers()
        }
    }

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart(/*existingSecurityPlan*/)
        onDispose {}
    }

    Scaffold(topBar = {
        TopAppBar(
            colors = TopAppBarDefaults.smallTopAppBarColors(
                containerColor = VaultColors.NavbarColor
            ),
            navigationIcon = {
                IconButton(onClick = {
                    viewModel.onBackActionClick()
                }) {
                    Icon(
                        imageVector = iconPair.first,
                        contentDescription = stringResource(id = iconPair.second),
                    )
                }
            },
            title = {
                // Title is a part of bottom aligned screen content
            },
            actions = {
                IconButton(onClick = {
                    Toast.makeText(context, "Show FAQ Web View", Toast.LENGTH_LONG).show()
                }) {
                    Icon(
                        painterResource(id = co.censo.shared.R.drawable.question),
                        contentDescription = "learn more"
                    )
                }
            })
    }) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            when {
                state.loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(72.dp),
                        strokeWidth = 5.dp
                    )
                }

                state.asyncError -> {
                    // FIXME add error cases with appropriate actions
                    DisplayError(
                        errorMessage = "Failed to invite approvers",
                        dismissAction = { viewModel.reset() },
                        retryAction = { viewModel.reset() },
                    )
                }

                else -> {
                    when (state.planSetupUIState) {
                        PlanSetupUIState.InviteApprovers ->
                            AddTrustedApproversUI(
                                welcomeFlow = welcomeFlow,
                                onInviteApproverSelected = { viewModel.onInvitePrimaryApprover() },
                                onSkipForNowSelected = { viewModel.skip() }
                            )


                        PlanSetupUIState.PrimaryApproverNickname -> {
                            AddApproverNicknameUI(
                                nickname = state.primaryApproverNickname,
                                enabled = state.primaryApproverNickname.isNotBlank(),
                                onNicknameChanged = viewModel::primaryAppoverNicknameChanged,
                                onSaveNickname = viewModel::onContinueWithPrimaryApprover
                            )
                        }
                        
                        
                        PlanSetupUIState.PrimaryApproverGettingLive -> {
                            GetLiveWithApproverUI(
                                nickname = state.primaryApproverNickname,
                                onContinueLive = {},
                                onResumeLater = {}
                            )
                        }

                        PlanSetupUIState.PrimaryApproverActivation -> TODO()
                        PlanSetupUIState.AddBackupApprover -> TODO()
                        PlanSetupUIState.BackupApproverNickname -> TODO()
                        PlanSetupUIState.BackupApproverGettingLive -> TODO()
                        PlanSetupUIState.BackupApproverActivation -> TODO()
                        PlanSetupUIState.Completed -> TODO()

                        /*SetupSecurityPlanScreen.AddApprovers ->
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

                    if (state.showCancelPlanSetupDialog) {
                        CancelEditPlanDialog(
                            paddingValues = paddingValues,
                            onDismiss = viewModel::dismissDialog,
                            onCancel = {
                                viewModel.clearEditingPlanData()
                                viewModel.dismissDialog()
                                navController.navigate(SharedScreen.HomeRoute.route) {
                                    launchSingleTop = true
                                    popUpToTop()
                                }
                            }
                        )
                    }

                    if (state.asyncError) {
                        if (state.createPolicySetupResponse is Resource.Error) {
                            DisplayError(
                                errorMessage = state.createPolicySetupResponse.getErrorMessage(
                                    context
                                ),
                                dismissAction = {
                                    viewModel.resetCreatePolicySetup()
                                },
                                retryAction = {
                                    viewModel.retryFacetec()
                                }
                            )
                        }
                    }*/

                    }
                }
            }
        }
    }
}