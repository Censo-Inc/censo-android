package co.censo.vault.presentation.guardian_invitation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.persistableBundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.vault.data.Resource
import co.censo.vault.data.model.OwnerState
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

    fun onStart() {
        retrieveUserState()
    }

    fun retrieveUserState() {
        state = state.copy(userResponse = Resource.Loading())
        viewModelScope.launch {
            val userResponse = ownerRepository.retrieveUser()

            if (userResponse is Resource.Success) {
                val guardianInvitationStatus = if (userResponse.data?.ownerState != null) {
                    when (userResponse.data.ownerState) {
                        is OwnerState.PolicySetup -> GuardianInvitationStatus.POLICY_SETUP
                        is OwnerState.Ready -> GuardianInvitationStatus.READY
                    }
                } else {
                    GuardianInvitationStatus.CREATE_POLICY
                }
                state = state.copy(guardianInviteStatus = guardianInvitationStatus)
            }

            state = state.copy(userResponse = userResponse)
        }
    }

    fun updateThreshold(value: Int) {
        if (value > 0 && value <= state.potentialGuardians.size) {
            state = state.copy(threshold = value)
        }
    }

    fun addGuardian() {
        if (state.potentialGuardians.size == MAX_GUARDIAN_LIMIT) {
            return
        }

        val potentialGuardian = "Guardian ${state.potentialGuardians.size + 1}"

        val guardians = state.potentialGuardians.toMutableList()
        guardians.add(potentialGuardian)
        state = state.copy(
            potentialGuardians = guardians,
            threshold = state.threshold + 1
        )
    }

    fun userSubmitGuardianSet() {
        triggerBiometry()
    }

    fun triggerBiometry() {
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

    fun createPolicy() {
        viewModelScope.launch {
            try {
                val policySetupHelper = ownerRepository.setupPolicy(
                    threshold = state.threshold,
                    guardians = state.potentialGuardians
                )

                val setupPolicyResponse = ownerRepository.createPolicy(policySetupHelper)

                state = state.copy(createPolicyResponse = setupPolicyResponse)

                if (setupPolicyResponse is Resource.Success) {
                    retrieveUserState()
                }
            } catch (e: Exception) {
                state = state.copy(createPolicyResponse = Resource.Error())
            }
        }
    }

    fun resetUserResponse() {
        state = state.copy(userResponse = Resource.Uninitialized)
    }

    fun resetCreatePolicyResource() {
        state = state.copy(createPolicyResponse = Resource.Uninitialized)
    }

    fun resetBiometryTrigger() {
        state = state.copy(bioPromptTrigger = Resource.Uninitialized)
    }



    private fun createGuardianDeepLinks() {

        viewModelScope.launch {
            val guardianDeepLinks = ownerRepository.retrieveGuardianDeepLinks(
                state.potentialGuardians, ""
            )

            state = state.copy(
                guardianDeepLinks = guardianDeepLinks,
                guardianInviteStatus = GuardianInvitationStatus.POLICY_SETUP
            )
        }
    }
}