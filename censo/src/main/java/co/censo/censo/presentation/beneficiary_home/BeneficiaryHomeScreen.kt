package co.censo.censo.presentation.beneficiary_home

import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import co.censo.censo.R
import co.censo.censo.presentation.beneficiary_home.ui.BeneficiaryHomeUI
import co.censo.censo.presentation.beneficiary_home.ui.SelectTakeoverApproverUI
import co.censo.censo.presentation.beneficiary_home.ui.TakeoverAcceptedRejectedUI
import co.censo.censo.presentation.biometry_reset.BiometryResetAction
import co.censo.censo.presentation.components.YesNoDialog
import co.censo.shared.data.Resource
import co.censo.shared.data.model.BeneficiaryPhase
import co.censo.shared.presentation.components.BasicScreen


@Composable
fun BeneficiaryHomeScreen(
    viewmodel: BeneficiaryHomeViewModel = hiltViewModel()
) {
    val state = viewmodel.state
    val context = LocalContext.current

    BasicScreen(
        loading = state.fullScreenLoading,
        error = state.error?.toErrorString(context),
        resetError = viewmodel::resetError,
        retry = viewmodel::retry
    ) {

        AnimatedContent(
            targetState = state.takeoverUIState,
            label = "",
        ) {
            when (it) {
                TakeoverUIState.Home -> BeneficiaryHomeUI(
                    loading = state.initiateTakeoverResponse is Resource.Loading,
                    initiateTakeover = viewmodel::initiateTakeover,
                    showSettings = {
                        //todo: add a settings nav path to beneficiary app
                    }
                )

                TakeoverUIState.TakeoverInitiated -> {
                    (state.ownerState?.phase as? BeneficiaryPhase.TakeoverInitiated)?.let { takeoverInitiated ->
                        val selectedApprover =
                            state.selectedApprover ?: takeoverInitiated.approverContactInfo.first()
                        SelectTakeoverApproverUI(
                            approvers = takeoverInitiated.approverContactInfo,
                            selectedApprover = selectedApprover.participantId,
                            takeoverId = takeoverInitiated.guid,
                            onSelectedApprover = viewmodel::approverSelected
                        )
                    }
                }

                TakeoverUIState.TakeoverRejected -> {
                    (state.ownerState?.phase as? BeneficiaryPhase.TakeoverRejected)?.let { takeoverRejected ->
                        TakeoverAcceptedRejectedUI(
                            accepted = false,
                            approverLabel = takeoverRejected.approverContactInfo.label,
                            countdownTime = null,
                            onCancelTakeover = viewmodel::showCancelDialog
                        )
                    }
                }

                TakeoverUIState.TakeoverTimelocked -> {
                    (state.ownerState?.phase as? BeneficiaryPhase.TakeoverTimelocked)?.let { takeoverTimeLocked ->
                        TakeoverAcceptedRejectedUI(
                            accepted = true,
                            approverLabel = takeoverTimeLocked.approverContactInfo.label,
                            countdownTime = takeoverTimeLocked.unlocksAt,
                            onCancelTakeover = viewmodel::cancelTakeover
                        )
                    }
                }
            }
        }

        if (state.triggerCancelTakeoverDialog) {
            YesNoDialog(
                title = stringResource(R.string.are_you_sure),
                message = stringResource(R.string.cancel_biometry_reset_dialog),
                onDismiss =  viewmodel::dismissCancelDialog,
                onConfirm =  viewmodel::confirmDialog
            )
        }
    }
}