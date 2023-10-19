package co.censo.vault.presentation.plan_setup

import InvitationId
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
import androidx.compose.material3.Text
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
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GuardianPhase
import co.censo.shared.presentation.components.DisplayError
import co.censo.vault.R
import co.censo.vault.presentation.VaultColors
import co.censo.vault.presentation.plan_setup.components.ActivateApproverUI
import co.censo.vault.presentation.plan_setup.components.AddApproverNicknameUI
import co.censo.vault.presentation.plan_setup.components.AddBackupApproverUI
import co.censo.vault.presentation.plan_setup.components.AddTrustedApproversUI
import co.censo.vault.presentation.plan_setup.components.GetLiveWithApproverUI
import co.censo.vault.presentation.plan_setup.components.SavedAndShardedUI
import kotlinx.coroutines.delay

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
        if (state.backArrowType == PlanSetupState.BackIconType.Back) Icons.Filled.ArrowBack to R.string.back
        else Icons.Filled.Clear to R.string.exit

    LaunchedEffect(key1 = state) {
        if (state.navigationResource is Resource.Success) {
            state.navigationResource.data?.let {
                navController.navigate(it)
                viewModel.resetNavigationResource()
            }
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
                Text(
                    text =
                    when (state.planSetupUIState) {
                        PlanSetupUIState.PrimaryApproverActivation -> stringResource(id = R.string.primary_approver)
                        PlanSetupUIState.BackupApproverActivation -> stringResource(id = R.string.backup_approver)
                        else -> ""
                    }
                )
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
                                onSkipForNowSelected = {
                                    Toast.makeText(context, "Skip for now", Toast.LENGTH_LONG).show()
                                }
                            )


                        PlanSetupUIState.PrimaryApproverNickname -> {
                            AddApproverNicknameUI(
                                nickname = state.primaryApprover.nickname,
                                enabled = state.primaryApprover.nickname.isNotBlank(),
                                onNicknameChanged = viewModel::primaryApproverNicknameChanged,
                                onSaveNickname = viewModel::onSavePrimaryApprover
                            )
                        }
                        
                        
                        PlanSetupUIState.PrimaryApproverGettingLive -> {
                            GetLiveWithApproverUI(
                                nickname = state.primaryApprover.nickname,
                                onContinueLive = viewModel::onGoLiveWithPrimaryApprover,
                                onResumeLater = {
                                    Toast.makeText(context, "Resume later", Toast.LENGTH_LONG).show()
                                }
                            )
                        }

                        PlanSetupUIState.PrimaryApproverActivation -> {
                            ActivateApproverUI(
                                isPrimaryApprover = true,
                                nickName = state.primaryApprover.nickname,
                                secondsLeft = state.primaryApprover.secondsLeft,
                                verificationCode = state.primaryApprover.totpCode,
                                guardianPhase = GuardianPhase.WaitingForCode(InvitationId("")),
                                deeplink = "",
                                storesLink = "Universal link to the App/Play stores"
                            )
                        }

                        PlanSetupUIState.AddBackupApprover -> {
                            AddBackupApproverUI(
                                onInviteBackupSelected = viewModel::onInviteBackupApprover,
                                onSaveAndFinishSelected = viewModel::saveAndFinish
                            )
                        }

                        PlanSetupUIState.BackupApproverNickname -> {
                            AddApproverNicknameUI(
                                nickname = state.backupApprover.nickname,
                                enabled = state.backupApprover.nickname.isNotBlank(),
                                onNicknameChanged = viewModel::backupApproverNicknameChanged,
                                onSaveNickname = viewModel::onContinueWithBackupApprover
                            )
                        }

                        PlanSetupUIState.BackupApproverGettingLive -> {
                            GetLiveWithApproverUI(
                                nickname = state.backupApprover.nickname,
                                onContinueLive = viewModel::onBackupApproverVerification,
                                onResumeLater = {
                                    Toast.makeText(context, "Resume later", Toast.LENGTH_LONG).show()
                                }
                            )
                        }

                        PlanSetupUIState.BackupApproverActivation -> TODO()

                        PlanSetupUIState.Completed -> {
                            SavedAndShardedUI(
                                seedPhraseNickname = "Yankee Hotel Foxtrot",
                                primaryApproverNickname = state.primaryApprover.nickname,
                                backupApproverNickname = state.backupApprover.nickname
                            )

                            LaunchedEffect(Unit) {
                                delay(5000)
                                viewModel.onFullyCompleted()
                            }
                        }
                    }
                }
            }
        }
    }
}