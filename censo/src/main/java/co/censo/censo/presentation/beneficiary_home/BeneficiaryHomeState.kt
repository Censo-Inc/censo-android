package co.censo.censo.presentation.beneficiary_home

import android.content.Context
import co.censo.censo.R
import co.censo.shared.data.Resource
import co.censo.shared.data.model.BeneficiaryApproverContactInfo
import co.censo.shared.data.model.BeneficiaryPhase
import co.censo.shared.data.model.CancelTakeoverApiResponse
import co.censo.shared.data.model.InitiateTakeoverApiResponse
import co.censo.shared.data.model.OwnerState

data class BeneficiaryHomeState(
    val ownerState: OwnerState.Beneficiary? = null,
    val takeoverUIState: TakeoverUIState = TakeoverUIState.Home,

    val selectedApprover: BeneficiaryApproverContactInfo? = null,

    val triggerCancelTakeoverDialog: Boolean = false,

    //API Calls
    val initiateTakeoverResponse: Resource<InitiateTakeoverApiResponse> = Resource.Uninitialized,
    val cancelTakeoverResponse: Resource<CancelTakeoverApiResponse> = Resource.Uninitialized,
) {
    val fullScreenLoading = cancelTakeoverResponse is Resource.Loading

    val error = when {
        initiateTakeoverResponse is Resource.Error -> BeneficiaryHomeError.InitiateFailed
        cancelTakeoverResponse is Resource.Error -> BeneficiaryHomeError.CancelFailed
        else -> null
    }
}

enum class TakeoverUIState {
    Home, TakeoverInitiated, TakeoverTimelocked, TakeoverRejected
}

enum class BeneficiaryHomeError {
    InitiateFailed, CancelFailed
}

fun BeneficiaryHomeError.toErrorString(context: Context) =
    when (this) {
        BeneficiaryHomeError.InitiateFailed -> context.getString(R.string.failed_to_initiate_takeover)
        BeneficiaryHomeError.CancelFailed -> context.getString(R.string.failed_to_cancel_takeover)
    }

fun OwnerState.Beneficiary.toBeneficiaryPhraseUIState(): TakeoverUIState =
    when (this.phase) {
        is BeneficiaryPhase.VerificationRejected -> TODO()
        is BeneficiaryPhase.WaitingForVerification -> TODO()
        is BeneficiaryPhase.Accepted,
        is BeneficiaryPhase.Activated -> TakeoverUIState.Home

        is BeneficiaryPhase.TakeoverAnotherDevice -> TODO()
        is BeneficiaryPhase.TakeoverAvailable -> TODO()
        is BeneficiaryPhase.TakeoverInitiated -> TakeoverUIState.TakeoverInitiated
        is BeneficiaryPhase.TakeoverRejected -> TakeoverUIState.TakeoverRejected
        is BeneficiaryPhase.TakeoverTimelocked -> TakeoverUIState.TakeoverTimelocked
        is BeneficiaryPhase.TakeoverVerificationPending -> TODO()
        is BeneficiaryPhase.TakeoverVerificationSignatureRejected -> TODO()
        is BeneficiaryPhase.TakeoverVerificationSignatureSubmitted -> TODO()
        is BeneficiaryPhase.TakeoverWaitingForVerificationSignature -> TODO()
    }