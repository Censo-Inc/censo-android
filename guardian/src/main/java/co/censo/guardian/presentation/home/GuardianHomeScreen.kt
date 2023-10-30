package co.censo.guardian.presentation.home

import Base58EncodedPrivateKey
import ParticipantId
import StandardButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.guardian.R
import co.censo.guardian.presentation.GuardianColors
import co.censo.guardian.presentation.components.ApproverCodeVerification
import co.censo.guardian.presentation.components.GuardianTopBar
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.TotpCodeView
import kotlinx.coroutines.delay
import co.censo.shared.presentation.cloud_storage.CloudStorageHandler
import co.censo.shared.util.projectLog
import io.github.novacrypto.base58.Base58

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianHomeScreen(
    navController: NavController,
    viewModel: GuardianHomeViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_START
            -> {
                viewModel.onStart()
            }

            Lifecycle.Event.ON_PAUSE -> {
                viewModel.onStop()
            }

            else -> Unit
        }
    }

    when {

        state.loading -> {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(72.dp)
                        .align(Alignment.Center),
                    strokeWidth = 8.dp,
                    color = Color.Black
                )
            }
        }

        state.asyncError -> {
            when {
                state.userResponse is Resource.Error -> {
                    DisplayError(
                        errorMessage = state.userResponse.getErrorMessage(context),
                        dismissAction = null,
                    ) { viewModel.retrieveApproverState() }
                }

                state.acceptGuardianResource is Resource.Error -> {
                    DisplayError(
                        errorMessage = state.acceptGuardianResource.getErrorMessage(context),
                        dismissAction = { viewModel.resetAcceptGuardianResource() },
                    ) { viewModel.acceptGuardianship() }
                }

                state.submitVerificationResource is Resource.Error -> {
                    DisplayError(
                        errorMessage = state.submitVerificationResource.getErrorMessage(context),
                        dismissAction = { viewModel.resetSubmitVerificationResource() },
                    ) { viewModel.submitVerificationCode() }
                }

                state.storeRecoveryTotpSecretResource is Resource.Error -> {
                    DisplayError(
                        errorMessage = state.storeRecoveryTotpSecretResource.getErrorMessage(context),
                        dismissAction = { viewModel.resetStoreRecoveryTotpSecretResource() },
                    ) { viewModel.storeRecoveryTotpSecret() }
                }

                state.approveRecoveryResource is Resource.Error -> {
                    DisplayError(
                        errorMessage = state.approveRecoveryResource.getErrorMessage(context),
                        dismissAction = { viewModel.resetApproveRecoveryResource() },
                    ) {
                        viewModel.resetApproveRecoveryResource()
                        viewModel.retrieveApproverState()
                    }
                }

                state.rejectRecoveryResource is Resource.Error -> {
                    DisplayError(
                        errorMessage = state.rejectRecoveryResource.getErrorMessage(context),
                        dismissAction = { viewModel.resetRejectRecoveryResource() },
                    ) {
                        viewModel.resetRejectRecoveryResource()
                        viewModel.retrieveApproverState()
                    }
                }

                state.savePrivateKeyToCloudResource is Resource.Error -> {
                    DisplayError(
                        errorMessage = state.savePrivateKeyToCloudResource.getErrorMessage(context),
                        dismissAction = { viewModel.createAndSaveGuardianKey() }
                    ) { viewModel.createAndSaveGuardianKey() }
                }

                else -> {
                    DisplayError(
                        errorMessage = stringResource(R.string.something_went_wrong),
                        dismissAction = null,
                    ) { viewModel.retrieveApproverState() }
                }
            }
        }

        else -> {

            if (state.showTopBarCancelConfirmationDialog) {
                AlertDialog(
                    onDismissRequest = viewModel::hideCloseConfirmationDialog,
                    text = {
                        Text(
                            modifier = Modifier.padding(8.dp),
                            text = stringResource(R.string.do_you_really_want_to_cancel),
                            color = GuardianColors.PrimaryColor,
                            textAlign = TextAlign.Center,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = viewModel::onTopBarCloseConfirmed
                        ) {
                            Text(stringResource(R.string.yes))
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = viewModel::hideCloseConfirmationDialog
                        ) {
                            Text(stringResource(R.string.no))
                        }
                    }
                )
            }

            Scaffold(
                topBar = {
                    GuardianTopBar(
                        uiState = state.guardianUIState,
                        onClose = viewModel::showCloseConfirmationDialog
                    )
                },
                content = { contentPadding ->

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.weight(0.3f))

                        when (state.guardianUIState) {

                            GuardianUIState.MISSING_INVITE_CODE -> {
                                InvitesOnly()
                            }

                            GuardianUIState.INVITE_READY -> {
                                InviteReady(
                                    onAccept = viewModel::acceptGuardianship,
                                    onCancel = viewModel::cancelOnboarding,
                                    enabled = state.acceptGuardianResource !is Resource.Loading
                                )
                            }

                            GuardianUIState.WAITING_FOR_CODE -> {
                                ApproverCodeVerification(
                                    value = state.verificationCode,
                                    onValueChanged = viewModel::updateVerificationCode,
                                    validCodeLength = TotpGenerator.CODE_LENGTH,
                                    label = stringResource(
                                        R.string.enter_the_digit_code_from_the_seed_phrase_owner,
                                        TotpGenerator.CODE_LENGTH
                                    ),
                                    isLoading = state.submitVerificationResource is Resource.Loading,
                                    isVerificationRejected = false,
                                    isWaitingForVerification = false,
                                )
                            }

                            GuardianUIState.WAITING_FOR_CONFIRMATION -> {
                                ApproverCodeVerification(
                                    value = state.verificationCode,
                                    onValueChanged = viewModel::updateVerificationCode,
                                    validCodeLength = TotpGenerator.CODE_LENGTH,
                                    label = stringResource(R.string.code_sent_to_owner_waiting_for_them_to_approve),
                                    isLoading = state.submitVerificationResource is Resource.Loading,
                                    isVerificationRejected = false,
                                    isWaitingForVerification = true,
                                )
                            }

                            GuardianUIState.CODE_REJECTED -> {
                                ApproverCodeVerification(
                                    value = state.verificationCode,
                                    onValueChanged = viewModel::updateVerificationCode,
                                    validCodeLength = TotpGenerator.CODE_LENGTH,
                                    label = stringResource(R.string.code_not_approved),
                                    isLoading = state.submitVerificationResource is Resource.Loading,
                                    isVerificationRejected = true,
                                    isWaitingForVerification = false,
                                )
                            }

                            GuardianUIState.COMPLETE -> {
                                Onboarded()
                            }

                            GuardianUIState.INVALID_PARTICIPANT_ID -> {
                                InvalidParticipantId()
                            }

                            GuardianUIState.ACCESS_REQUESTED -> {
                                RecoveryRequested(onContinue = viewModel::storeRecoveryTotpSecret )
                            }

                            GuardianUIState.ACCESS_WAITING_FOR_TOTP_FROM_OWNER -> {
                                OwnerCodeVerification(
                                    totpCode = state.recoveryTotp?.code,
                                    secondsLeft = state.recoveryTotp?.currentSecond
                                )
                            }

                            GuardianUIState.ACCESS_VERIFYING_TOTP_FROM_OWNER -> {
                                VerifyingOwnerCode()
                            }

                            GuardianUIState.ACCESS_APPROVED -> {
                                AccessApproved(
                                    onClose = viewModel::resetApproveRecoveryResource
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(0.7f))
                    }
                }
            )

            if (state.cloudStorageAction.triggerAction) {
                val privateKey =
                    if (state.cloudStorageAction.action == CloudStorageActions.UPLOAD) {
                        viewModel.getPrivateKeyForUpload()
                    } else null


                if (state.savePrivateKeyToCloudResource is Resource.Loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(72.dp)
                                .align(Alignment.Center),
                            strokeWidth = 8.dp,
                            color = Color.Black
                        )
                    }
                }

                CloudStorageHandler(
                    actionToPerform = state.cloudStorageAction.action,
                    participantId = ParticipantId(state.participantId),
                    privateKey = privateKey,
                    onActionSuccess = { base58EncodedPrivateKey ->
                        projectLog(message = "Cloud Storage action success")
                        viewModel.handleCloudStorageActionSuccess(base58EncodedPrivateKey, state.cloudStorageAction.action)
                    },
                    onActionFailed = { exception ->
                        projectLog(message = "Cloud Storage action failed")
                        viewModel.handleCloudStorageActionFailure(
                            exception,
                            state.cloudStorageAction.action
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun VerifyingOwnerCode() {
    Text(
        modifier = Modifier.padding(horizontal = 30.dp),
        text = stringResource(R.string.verifying_code),
        textAlign = TextAlign.Center,
        fontSize = 18.sp
    )
}

@Composable
private fun OwnerCodeVerification(
    totpCode: String?,
    secondsLeft: Int?
) {
    if (totpCode == null || secondsLeft == null) {
        Text(
            text = stringResource(R.string.loading),
            textAlign = TextAlign.Center,
            fontSize = 18.sp
        )
    } else {
        Text(
            modifier = Modifier.padding(horizontal = 30.dp),
            text = stringResource(
                R.string.tell_seed_phrase_owner_this_digit_code_to_approve_their_access,
                TotpGenerator.CODE_LENGTH
            ),
            textAlign = TextAlign.Center,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(30.dp))

        TotpCodeView(
            totpCode,
            secondsLeft,
            GuardianColors.PrimaryColor
        )
    }
}

@Composable
private fun InvalidParticipantId() {
    Text(
        modifier = Modifier.padding(horizontal = 30.dp),
        text = stringResource(R.string.link_you_have_opened_does_not_appear_to_be_correct_please_contact_seed_phrase_owner),
        textAlign = TextAlign.Center,
        fontSize = 18.sp
    )
}

@Composable
private fun AccessApproved(
    onClose: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(5000)
        onClose()
    }

    Text(
        modifier = Modifier.padding(16.dp),
        text = stringResource(R.string.access_approved),
        color = GuardianColors.PrimaryColor,
        textAlign = TextAlign.Center,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun Onboarded() {
    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = stringResource(R.string.you_are_fully_set),
        color = GuardianColors.PrimaryColor,
        textAlign = TextAlign.Center,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(30.dp))

    Text(
        modifier = Modifier.padding(horizontal = 40.dp),
        text = stringResource(R.string.when_needed_the_seed_phrase_owner_will_get_in_touch_with_you_to_approve_their_access),
        color = GuardianColors.PrimaryColor,
        textAlign = TextAlign.Center,
        fontSize = 18.sp,
        fontWeight = FontWeight.Normal
    )
}

@Composable
private fun InvitesOnly() {
    Text(
        modifier = Modifier.padding(horizontal = 30.dp),
        text = stringResource(R.string.this_application_can_only_be_used_by_invitation_please_click_the_invite_link_you_received_from_the_seed_phrase_owner),
        textAlign = TextAlign.Center,
        fontSize = 18.sp
    )
}

@Composable
private fun InviteReady(
    onAccept: () -> Unit,
    onCancel: () -> Unit,
    enabled: Boolean
) {
    Text(
        modifier = Modifier.padding(horizontal = 30.dp),
        text = stringResource(R.string.you_have_been_invited_to_become_an_approver),
        textAlign = TextAlign.Center,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium
    )

    Spacer(modifier = Modifier.height(48.dp))

    StandardButton(
        modifier = Modifier.padding(horizontal = 24.dp),
        color = GuardianColors.PrimaryColor,
        borderColor = Color.White,
        border = false,
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 20.dp),
        onClick = onAccept,
        enabled = enabled
    )
    {
        Text(
            text = stringResource(R.string.accept_invitation),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    TextButton(onClick = onCancel) {
        Text(
            text = stringResource(R.string.close),
            color = Color.Black
        )
    }
}

@Composable
private fun RecoveryRequested(
    onContinue: () -> Unit
) {
    Text(
        modifier = Modifier.padding(horizontal = 30.dp),
        text = stringResource(R.string.seed_phrase_owner_has_requested_access_approval),
        textAlign = TextAlign.Center,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium
    )

    Spacer(modifier = Modifier.height(48.dp))

    StandardButton(
        modifier = Modifier.padding(horizontal = 24.dp),
        color = GuardianColors.PrimaryColor,
        borderColor = Color.White,
        border = false,
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 20.dp),
        onClick = onContinue,
    )
    {
        Text(
            text = "Continue",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
}