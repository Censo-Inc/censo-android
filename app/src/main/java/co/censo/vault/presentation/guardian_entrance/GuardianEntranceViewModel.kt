package co.censo.vault.presentation.guardian_entrance

import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.vault.data.repository.GuardianRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GuardianEntranceViewModel @Inject constructor(
    private val guardianRepository: GuardianRepository
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

        //TODO: Set biometry prompt to get signed timestamp for registerGuardian api call
    }

    fun registerGuardian() {
        viewModelScope.launch {
            val registerGuardianResponse = guardianRepository.registerGuardian(
                intermediateKey = state.intermediateKey,
                participantId = state.participantId
            )

            state = state.copy(
                registerGuardianResource = registerGuardianResponse
            )
        }
    }

}