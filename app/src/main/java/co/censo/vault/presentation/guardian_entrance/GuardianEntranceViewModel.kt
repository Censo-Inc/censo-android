package co.censo.vault.presentation.guardian_entrance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.vault.data.Resource
import co.censo.vault.data.repository.GuardianRepository
import co.censo.vault.data.repository.OwnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GuardianEntranceViewModel @Inject constructor(
    private val guardianRepository: GuardianRepository,
    private val ownerRepository: OwnerRepository
) : ViewModel() {

    var state by mutableStateOf(GuardianEntranceState())
        private set

    fun onStart(args: GuardianEntranceArgs) {
        if (args.isDataMissing()) {
            //TODO: Set error state and let user know that they cannot continue onboarding process for guardianship
        }

        state = state.copy(
            participantId = args.participantId,
            ownerDevicePublicKey = args.ownerDevicePublicKey,
            intermediateKey = args.intermediateKey
        )

        triggerBiometryPrompt()
    }

    private fun triggerBiometryPrompt() {
        state = state.copy(bioPromptTrigger = Resource.Success(Unit))
    }

    fun onBiometryApproved() {
        state = try {
            ownerRepository.saveValidTimestamp()
            registerGuardian()

            state.copy(bioPromptTrigger = Resource.Uninitialized)
        } catch (e: Exception) {
            //TODO: Log exception with raygun
            state.copy(bioPromptTrigger = Resource.Error())

        }
    }

    fun onBiometryFailed() {
        state = state.copy(bioPromptTrigger = Resource.Error())
    }

    fun registerGuardian() {
        viewModelScope.launch {
            val registerGuardianResponse = guardianRepository.registerGuardian(
                intermediateKey = state.intermediateKey,
                participantId = state.participantId
            )

            if (registerGuardianResponse is Resource.Success) {
                state = state.copy(guardianStatus = GuardianStatus.ENTER_VERIFICATION_CODE)
            }

            state = state.copy(
                registerGuardianResource = registerGuardianResponse
            )
        }
    }

    fun submitVerificationCode() {
        val verificationCode = state.verificationCode
    }

}