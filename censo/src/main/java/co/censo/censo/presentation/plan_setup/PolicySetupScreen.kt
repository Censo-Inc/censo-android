package co.censo.censo.presentation.plan_setup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.GetLiveWithUserUI
import co.censo.censo.R
import co.censo.censo.presentation.Screen
import co.censo.censo.presentation.plan_setup.components.ActivateApproverUI
import co.censo.censo.presentation.plan_setup.components.ApproverNicknameUI
import co.censo.shared.data.model.ApproverStatus
import co.censo.shared.presentation.components.LargeLoading
import co.censo.shared.util.LinksUtil
import co.censo.shared.util.projectLog

enum class PolicySetupAction(val threshold: UInt) {
    AddApprovers(2U), RemoveApprovers(1U)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolicySetupScreen(
    navController: NavController,
    policySetupAction: PolicySetupAction,
    viewModel: PolicySetupViewModel = hiltViewModel(),
) {
    val state = viewModel.state

    val iconPair = when (state.backArrowType) {
        PolicySetupState.BackIconType.Back -> Icons.Filled.ArrowBack to R.string.back
        PolicySetupState.BackIconType.Exit -> Icons.Filled.Clear to R.string.exit
        else -> null
    }

    LaunchedEffect(key1 = state) {
        if (state.navigationResource is Resource.Success) {
            state.navigationResource.data?.let {
                navController.navigate(it)
                viewModel.resetNavigationResource()
            }
        }

        if (state.replacePolicy is Resource.Success) {
            viewModel.resetReplacePolicy()
            //Pop back stack entirely from here
            val (route, popUpToRoute) = if (state.policySetupAction == PolicySetupAction.AddApprovers) {
                Pair(
                    Screen.ReplacePolicyRoute.addApproversRoute(),
                    Screen.PolicySetupRoute.addApproversRoute()
                )
            } else {
                Pair(
                    Screen.ReplacePolicyRoute.removeApproversRoute(),
                    Screen.PolicySetupRoute.removeApproversRoute()
                )
            }

            projectLog(message = "Navigating with route: $route")
            navController.navigate(route) {
                popUpTo(popUpToRoute) {
                    inclusive = true
                }
            }
        }
    }

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                viewModel.onCreate(policySetupAction)
            }

            Lifecycle.Event.ON_RESUME -> {
                viewModel.onResume()
            }

            Lifecycle.Event.ON_PAUSE -> {
                viewModel.onStop()
            }

            else -> Unit
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            navigationIcon = {
                when (iconPair) {
                    null -> {}
                    else -> {
                        IconButton(
                            onClick = {
                                viewModel.receivePlanAction(PolicySetupScreenAction.BackClicked)
                            }) {
                            Icon(
                                imageVector = iconPair.first,
                                contentDescription = stringResource(id = iconPair.second),
                            )
                        }
                    }
                }
            },
            title = {
                Text(
                    text =
                    when (state.policySetupUIState) {
                        PolicySetupUIState.ApproverActivation_5,
                        PolicySetupUIState.EditApproverNickname_3 ->
                            stringResource(R.string.add_approver_title)

                        else -> ""
                    }
                )
            }
        )
    }) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            when {
                state.loading -> LargeLoading(fullscreen = true)

                state.asyncError -> {
                    if (state.userResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to retrieve user information, try again.",
                            dismissAction = {
                                viewModel.resetUserResponse()
                                viewModel.receivePlanAction(PolicySetupScreenAction.Retry)
                            },
                            retryAction = {
                                viewModel.resetUserResponse()
                                viewModel.receivePlanAction(PolicySetupScreenAction.Retry)
                            },
                        )
                    } else if (state.createPolicySetupResponse is Resource.Error) {
                        DisplayError(
                            errorMessage = "Failed to create policy, try again",
                            dismissAction = {
                                viewModel.resetCreatePolicySetupResponse()
                                viewModel.receivePlanAction(PolicySetupScreenAction.Retry)
                            },
                            retryAction = {
                                viewModel.resetCreatePolicySetupResponse()
                                viewModel.receivePlanAction(PolicySetupScreenAction.Retry)
                            },
                        )
                    } else {
                        DisplayError(
                            errorMessage = "Something went wrong, please try again.",
                            dismissAction = { viewModel.receivePlanAction(PolicySetupScreenAction.Retry) },
                            retryAction = { viewModel.receivePlanAction(PolicySetupScreenAction.Retry) },
                        )
                    }
                }

                else -> {
                    when (state.policySetupUIState) {
                        PolicySetupUIState.Initial_1 -> LargeLoading(
                            fullscreen = true
                        )

                        PolicySetupUIState.ApproverNickname_2 -> {
                            ApproverNicknameUI(
                                isFirstApprover = state.primaryApprover?.status !is ApproverStatus.Confirmed,
                                nickname = state.editedNickname,
                                enabled = state.editedNicknameValid,
                                nicknameIsTooLong = state.editedNicknameIsTooLong,
                                onNicknameChanged = {
                                    viewModel.receivePlanAction(
                                        PolicySetupScreenAction.ApproverNicknameChanged(
                                            it
                                        )
                                    )
                                },
                                onSaveNickname = {
                                    viewModel.receivePlanAction(PolicySetupScreenAction.SaveApproverAndSavePolicy)
                                }
                            )
                        }

                        PolicySetupUIState.EditApproverNickname_3 -> {
                            ApproverNicknameUI(
                                isFirstApprover = state.primaryApprover?.status !is ApproverStatus.Confirmed,
                                isRename = true,
                                nickname = state.editedNickname,
                                enabled = state.editedNicknameValid,
                                nicknameIsTooLong = state.editedNicknameIsTooLong,
                                onNicknameChanged = {
                                    viewModel.receivePlanAction(
                                        PolicySetupScreenAction.ApproverNicknameChanged(
                                            it
                                        )
                                    )
                                },
                                onSaveNickname = {
                                    viewModel.receivePlanAction(PolicySetupScreenAction.EditApproverAndSavePolicy)
                                }
                            )
                        }

                        //Really light screen. Just moves us to next UI or let's user back out.
                        PolicySetupUIState.ApproverGettingLive_4 -> {
                            GetLiveWithUserUI(
                                title = "${stringResource(R.string.activate_approver)} ${state.editedNickname}",
                                message = stringResource(
                                    R.string.activate_your_approver_message,
                                    state.editedNickname
                                ),
                                activatingApprover = true,
                                onContinueLive = {
                                    viewModel.receivePlanAction(PolicySetupScreenAction.GoLiveWithApprover)
                                },
                                onResumeLater = {
                                    viewModel.receivePlanAction(PolicySetupScreenAction.BackClicked)
                                }
                            )
                        }

                        //Verify approver while approver does full onboarding
                        PolicySetupUIState.ApproverActivation_5 -> {
                            ActivateApproverUI(
                                prospectApprover = state.activatingApprover,
                                secondsLeft = state.secondsLeft,
                                verificationCode = state.approverCodes[state.activatingApprover?.participantId]
                                    ?: "",
                                storesLink = LinksUtil.CENSO_APPROVER_STORE_LINK,
                                onContinue = {
                                    viewModel.receivePlanAction(PolicySetupScreenAction.ApproverConfirmed)
                                },
                                onEditNickname = {
                                    viewModel.receivePlanAction(PolicySetupScreenAction.EditApproverNickname)
                                }
                            )
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}