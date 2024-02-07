package co.censo.censo.presentation.beneficiary_owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import co.censo.censo.R
import co.censo.censo.presentation.beneficiary_owner.beneficiary_owner_ui.ActivateBeneficiaryUI
import co.censo.censo.presentation.beneficiary_owner.beneficiary_owner_ui.ActiveBeneficiaryUI
import co.censo.censo.presentation.beneficiary_owner.beneficiary_owner_ui.AddBeneficiaryLabelUI
import co.censo.censo.presentation.beneficiary_owner.beneficiary_owner_ui.NoBeneficiaryTabScreen
import co.censo.shared.DeepLinkURI
import co.censo.shared.data.Resource
import co.censo.shared.data.model.BeneficiaryStatus
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.components.GetLiveWithUserUI
import co.censo.shared.presentation.components.LargeLoading
import co.censo.shared.util.LinksUtil.CENSO_BENEFICIARY_APP_STORE_LINK

@Composable
fun BeneficiaryHomeScreen(
    viewModel: BeneficiaryOwnerViewModel = hiltViewModel()
) {

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> viewModel.onStart()
            Lifecycle.Event.ON_RESUME -> viewModel.onResume()
            Lifecycle.Event.ON_PAUSE -> viewModel.onPause()
            else -> Unit
        }
    }

    val state = viewModel.state

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
    ) {

        val ownerState = state.ownerState

        if (ownerState == null) {
            //this should not happen. We get owner state in entrance VM every time we enter app.
            LargeLoading(fullscreen = true)
        } else {

            when (state.beneficiaryOwnerUIState) {
                BeneficiaryOwnerUIState.None -> {
                    NoBeneficiaryTabScreen(addBeneficiaryEnabled = state.ownerState.policy.approvers.size == 3) {
                        viewModel.moveUserThroughUI(BeneficiaryOwnerUIState.AddLabel)
                    }
                }

                BeneficiaryOwnerUIState.AddLabel -> {
                    AddBeneficiaryLabelUI(
                        label = state.beneficiaryLabel,
                        enabled = state.beneficiaryLabel.isNotEmpty() && state.inviteBeneficiaryResource !is Resource.Loading,
                        loading = state.inviteBeneficiaryResource is Resource.Loading,
                        failedToCreateBeneficiary = state.createBeneficiaryError,
                        onLabelChanged = viewModel::updateBeneficiaryLabel,
                        onCreateBeneficiary = viewModel::inviteBeneficiary,
                        dismissError = viewModel::dismissCreateBeneficiaryError
                    )
                }

                BeneficiaryOwnerUIState.VerifyNow -> {
                    val label = state.ownerState.policy.beneficiary?.label
                        ?: stringResource(id = R.string.beneficiary)
                    GetLiveWithUserUI(
                        title = "Verify $label",
                        message = stringResource(
                            R.string.activate_your_beneficiary_message,
                            label
                        ),
                        buttonText = stringResource(id = R.string.verify_now),
                        onContinueLive = {
                            viewModel.moveUserThroughUI(BeneficiaryOwnerUIState.ActivateBeneficiary)
                        },
                        onResumeLater = {
                            viewModel.moveUserThroughUI(BeneficiaryOwnerUIState.None)
                        }
                    )
                }

                else -> {
                    val beneficiary = state.ownerState.policy.beneficiary

                    when (val status = state.ownerState.policy.beneficiary?.status) {
                        null -> {
                            NoBeneficiaryTabScreen(addBeneficiaryEnabled = state.ownerState.policy.approvers.size == 3) {
                                viewModel.moveUserThroughUI(BeneficiaryOwnerUIState.AddLabel)
                            }
                        }

                        is BeneficiaryStatus.Activated -> {
                            ActiveBeneficiaryUI(
                                label = beneficiary?.label ?: "",
                                activatedStatus = status
                            ) {
                                //todo: Remove beneficiary
                            }
                        }

                        else -> {
                            ActivateBeneficiaryUI(
                                beneficiary = beneficiary!!,
                                deeplink = DeepLinkURI.createBeneficiaryDeeplink((status as? BeneficiaryStatus.Initial)?.invitationId?.value),
                                secondsLeft = state.secondsLeft,
                                verificationCode = state.beneficiaryCode,
                                storesLink = CENSO_BENEFICIARY_APP_STORE_LINK
                            ) {

                            }
                        }
                    }
                }
            }
        }
    }
}

