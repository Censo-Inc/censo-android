package co.censo.vault.presentation.guardian_invitation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.vault.data.Resource
import co.censo.vault.data.model.Guardian
import co.censo.vault.data.model.GuardianStatus
import co.censo.vault.data.repository.OwnerRepository
import co.censo.vault.presentation.home.Screen
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
            email = "",
            status = GuardianStatus.Invited,
            null
        )

        val guardians = state.guardians.toMutableList()
        guardians.add(guardian)
        state = state.copy(
            guardians = guardians,
            threshold = state.threshold + 1
        )
    }

    fun createKeysAndShares() {
        viewModelScope.launch {
            val shares = ownerRepository.createKeysAndShareInfo(state.guardians)

            if (shares.isNotEmpty()) {
                createGuardianDeepLinks()
            }
        }
    }

    private fun createGuardianDeepLinks() {

        viewModelScope.launch {
            val guardianDeepLinks = ownerRepository.retrieveGuardianDeepLinks()

            state = state.copy(
                guardianDeepLinks = guardianDeepLinks,
                guardianInviteStatus = GuardianInvitationStatus.INVITE_GUARDIANS
            )
        }
    }
}