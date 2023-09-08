package co.censo.vault.presentation.guardian_invitation

import Base64EncodedData
import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.vault.data.Resource
import co.censo.vault.data.model.Guardian
import co.censo.vault.data.model.GuardianStatus
import co.censo.vault.data.repository.OwnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GuardianInvitationViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository
) : ViewModel() {

    var state by mutableStateOf(GuardianInvitationState())
        private set

    companion object {
        //Do we want to limit how many guardians an owner can add?
        const val MAX_GUARDIAN_LIMIT = 5
        const val MIN_GUARDIAN_LIMIT = 3
    }

    fun updateThreshold(value: Int) {
        if (value > 0 && value <= state.guardians.size) {
            state = state.copy(threshold = value)
        }
    }

    fun addGuardian() {
        if (state.guardians.size == MAX_GUARDIAN_LIMIT) {
            return
        }

        val guardian = Guardian(
            name = "Guardian ${state.guardians.size + 1}",
            status = GuardianStatus.Invited,
            participantId = ParticipantId(),
            encryptedShard = Base64EncodedData(),
            signature = null,
            timeMillis = null
        )

        val guardians = state.guardians.toMutableList()
        guardians.add(guardian)
        state = state.copy(
            guardians = guardians,
            threshold = state.threshold + 1
        )
    }

    fun userSubmitGuardianSet() {
        triggerBiometry()
    }

    private fun triggerBiometry() {
        state = state.copy(
            bioPromptTrigger = Resource.Success(Unit),
        )
    }

    fun onBiometryApproved() {
        state = state.copy(bioPromptTrigger = Resource.Uninitialized)

        createPolicy()
    }

    fun onBiometryFailed() {
        state = state.copy(bioPromptTrigger = Resource.Error())
    }

    private fun createPolicy() {
        viewModelScope.launch {
            val policySetupHelper = ownerRepository.setupPolicy(
                threshold = state.threshold,
                guardians = state.guardians
            )

            val setupPolicyResponse = ownerRepository.createPolicy(policySetupHelper)


            if (setupPolicyResponse is Resource.Success) {
                createGuardianDeepLinks()
            }
        }
    }

    private fun createGuardianDeepLinks() {

        viewModelScope.launch {
            val guardianDeepLinks = ownerRepository.retrieveGuardianDeepLinks(state.guardians)

            state = state.copy(
                guardianDeepLinks = guardianDeepLinks,
                guardianInviteStatus = GuardianInvitationStatus.INVITE_GUARDIANS
            )
        }
    }
}