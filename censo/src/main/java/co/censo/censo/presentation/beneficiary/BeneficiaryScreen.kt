package co.censo.censo.presentation.beneficiary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.model.BeneficiaryPhase
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.presentation.components.CodeVerificationStatus
import co.censo.shared.presentation.components.CodeVerificationUI
import co.censo.shared.presentation.components.DisplayError

@Composable
fun BeneficiaryScreen(
    viewModel: BeneficiaryViewModel = hiltViewModel()
) {

    val state = viewModel.state

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> viewModel.onStart()
            Lifecycle.Event.ON_STOP -> viewModel.onStop()
            else -> Unit
        }
    }

    LaunchedEffect(key1 = state) {
        //TODO: we should be kicking the user out if they have not accepted or owner state is not beneficiary...
    }

    Box(modifier = Modifier.fillMaxSize().background(color = Color.White)) {
        val beneficiaryState = state.beneficiaryData

        beneficiaryState?.let {
            when (it.phase) {
                BeneficiaryPhase.Accepted,
                BeneficiaryPhase.VerificationRejected,
                BeneficiaryPhase.WaitingForVerification -> {

                    val verificationStatus = when {
                        it.phase is BeneficiaryPhase.VerificationRejected -> CodeVerificationStatus.Rejected
                        it.phase is BeneficiaryPhase.WaitingForVerification || state.manuallyLoadingVerificationCode -> CodeVerificationStatus.Waiting
                        else -> CodeVerificationStatus.Initial
                    }

                    CodeVerificationUI(
                        isLoading = state.apiResponse is Resource.Loading || state.manuallyLoadingVerificationCode,
                        codeVerificationStatus = verificationStatus,
                        validCodeLength = TotpGenerator.CODE_LENGTH,
                        value = state.verificationCode,
                        onValueChanged = viewModel::updateVerificationCode
                    )
                }

                BeneficiaryPhase.Activated -> {
                    BeneficiaryActivatedUI {
                        //todo: leave screen and show base Beneficiary UI
                    }
                }
            }
        }
    }

    if (!state.error.isNullOrEmpty()) {
        DisplayError(
            errorMessage = state.error,
            dismissAction = viewModel::resetErrorState,
            retryAction = null
        )
    }
}