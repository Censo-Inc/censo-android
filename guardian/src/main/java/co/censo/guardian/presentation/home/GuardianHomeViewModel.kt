package co.censo.guardian.presentation.home

import InvitationId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GuardianState
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.repository.GuardianRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.projectLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GuardianHomeViewModel @Inject constructor(
    private val guardianRepository: GuardianRepository,
    private val ownerRepository: OwnerRepository
) : ViewModel() {

    var state by mutableStateOf(GuardianHomeState())
        private set

    fun onStart() {
        retrieveUserState()
    }

    fun updateOwnerState(ownerState: OwnerState?, guardianStates: List<GuardianState>?) {
        state = state.copy(
            ownerState = ownerState ?: state.ownerState,
            guardianStates = guardianStates ?: state.guardianStates
        )
    }

    fun retrieveUserState() {
        state = state.copy(userResponse = Resource.Loading())
        viewModelScope.launch {
            val userResponse = ownerRepository.retrieveUser()

            if (userResponse is Resource.Success) {
                updateOwnerState(userResponse.data?.ownerState, userResponse.data?.guardianStates)
                projectLog(message = "User Response: ${userResponse.data}")
                state = state.copy(
                    userResponse = userResponse,
                    guardianUIState = GuardianUIState.USER_LOADED
                )
                checkIfGuardianHasInvitationCode()
            } else {
                state = state.copy(userResponse = userResponse)
            }
        }
    }

    fun checkIfGuardianHasInvitationCode() {
        val invitationCode = guardianRepository.retrieveInvitationId()

        if (invitationCode.isNotEmpty()) {
            state = state.copy(
                invitationId = InvitationId(invitationCode),
                guardianUIState = GuardianUIState.HAS_INVITE_CODE
            )
        }
    }

    fun acceptGuardianship() {
        state = state.copy(acceptGuardianResource = Resource.Loading())

        viewModelScope.launch {
            val acceptResource = guardianRepository.acceptGuardianship(
                invitationId = state.invitationId,
            )

            state = if (acceptResource is Resource.Success) {
                state.copy(
                    guardianUIState = GuardianUIState.ACCEPTED_INVITE,
                    acceptGuardianResource = acceptResource
                )
            } else {
                state.copy(acceptGuardianResource = acceptResource)
            }
        }
    }

    fun declineGuardianship() {
        state = state.copy(declineGuardianResource = Resource.Loading())

        viewModelScope.launch {
            val declineResource = guardianRepository.declineGuardianship(
                invitationId = state.invitationId,
            )

            state = if (declineResource is Resource.Success) {
                state.copy(
                    guardianUIState = GuardianUIState.ACCEPTED_INVITE,
                    declineGuardianResource = declineResource
                )
            } else {
                state.copy(declineGuardianResource = declineResource)
            }
        }
    }

    fun submitVerificationCode() {
        state = state.copy(submitVerificationResource = Resource.Loading())

        viewModelScope.launch {
            //todo: Have user input this
            val submitVerificationResource = guardianRepository.submitGuardianVerification(
                invitationId = state.invitationId.value,
                verificationCode = "123456"
            )

            state = if (submitVerificationResource is Resource.Success) {
                state.copy(
                    guardianUIState = GuardianUIState.VERIFIED,
                    submitVerificationResource = submitVerificationResource
                )
            } else {
                state.copy(submitVerificationResource = submitVerificationResource)
            }
        }
    }
}