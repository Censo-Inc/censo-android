package co.censo.guardian.presentation.guardian_entrance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import co.censo.shared.data.repository.GuardianRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GuardianEntranceViewModel @Inject constructor(
    private val guardianRepository: GuardianRepository
) : ViewModel() {

    var state by mutableStateOf(GuardianEntranceState())
        private set

    fun loginGuardian () {
        //TODO: Implement login with grants to google cloud
        state = state.copy(guardianStatus = GuardianStatus.REGISTER)
    }

    fun registerGuardian() {
        //TODO: Implement API call to register guardian device
        state = state.copy(guardianStatus = GuardianStatus.DISPLAY_QR_CODE_FOR_SCANNING)
    }
}