package co.censo.censo.presentation.beneficiary_home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.model.BeneficiaryApproverContactInfo
import co.censo.shared.data.model.BeneficiaryPhase
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.repository.BeneficiaryRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.VaultCountDownTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BeneficiaryHomeViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val beneficiaryRepository: BeneficiaryRepository,
    private val approverAcceptedTakeoverTimer: VaultCountDownTimer,
) : ViewModel() {

    var state by mutableStateOf(BeneficiaryHomeState())
        private set

    fun onStart() {
        val ownerState = ownerRepository.getOwnerStateValue()
        onOwnerStateUpdate(ownerState, true)

        (ownerState as? OwnerState.Beneficiary)?.let {
            if (it.phase is BeneficiaryPhase.TakeoverInitiated) {
                startTakeoverRejectAcceptPolling()
            }
        }
    }

    fun onStop() {
        approverAcceptedTakeoverTimer.stop()
    }

    private fun onOwnerStateUpdate(ownerState: OwnerState, updateUIState: Boolean) {
        (ownerState as? OwnerState.Beneficiary)?.let {
            val uiState = if (updateUIState) {
                ownerState.toBeneficiaryPhraseUIState()
            } else {
                state.takeoverUIState
            }

            state = state.copy(
                takeoverUIState = uiState,
                ownerState = ownerState
            )
        }
    }

    fun initiateTakeover() {
        if (state.initiateTakeoverResponse is Resource.Loading) return

        state = state.copy(initiateTakeoverResponse = Resource.Loading)

        viewModelScope.launch {
            val initiateResponse = beneficiaryRepository.initiateTakeover()

            if (initiateResponse is Resource.Success) {
                state = state.copy(
                    takeoverUIState = TakeoverUIState.TakeoverInitiated
                )
                onOwnerStateUpdate(initiateResponse.data.ownerState, true)
                startTakeoverRejectAcceptPolling()
            }

            state = state.copy(initiateTakeoverResponse = initiateResponse)
        }
    }

    fun cancelTakeover() {
        if (state.cancelTakeoverResponse is Resource.Loading) return

        state = state.copy(cancelTakeoverResponse = Resource.Loading)

        viewModelScope.launch {
            val cancelResponse = beneficiaryRepository.cancelTakeover()

            if (cancelResponse is Resource.Success) {
                state = state.copy(
                    takeoverUIState = TakeoverUIState.Home
                )
                onOwnerStateUpdate(cancelResponse.data.ownerState, true)
            }

            state = state.copy(cancelTakeoverResponse = cancelResponse)
        }
    }

    fun approverSelected(beneficiaryApproverContactInfo: BeneficiaryApproverContactInfo) {
        state = state.copy(selectedApprover = beneficiaryApproverContactInfo)
    }

    private fun startTakeoverRejectAcceptPolling() {
        approverAcceptedTakeoverTimer.start(5000, skipFirstTick = true) {
            retrieveOwnerState()
        }
    }

    fun retrieveOwnerState() {
        viewModelScope.launch {
            val ownerState = ownerRepository.retrieveUser()

            ownerState.onSuccess {
                onOwnerStateUpdate(it.ownerState, true)
            }
        }
    }

    fun resetError() {
        state = state.copy(
            initiateTakeoverResponse = Resource.Uninitialized,
            cancelTakeoverResponse = Resource.Uninitialized
        )
    }

    fun retry() {
        val error = state.error
        resetError()
        when (error) {
            BeneficiaryHomeError.InitiateFailed -> initiateTakeover()
            BeneficiaryHomeError.CancelFailed -> cancelTakeover()
            null -> initiateTakeover()
        }
    }
}