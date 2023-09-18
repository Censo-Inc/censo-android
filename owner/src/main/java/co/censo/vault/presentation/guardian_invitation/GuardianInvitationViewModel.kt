package co.censo.vault.presentation.guardian_invitation

import InvitationId
import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.repository.OwnerRepository
import co.censo.vault.util.vaultLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
        
        val enumerateGuardiansGetUserApiResponse = GetUserApiResponse(
            userGuid = "ei",
            biometricVerificationRequired = false,
            guardianStates = listOf(),
            ownerState = OwnerState.GuardianSetup(
                guardians = listOf()
            )

        )
    }

    fun onStart() {
        retrieveUserState()
    }

    fun onUserCreatedGuardianSet() {
        state = state.copy(guardianInviteStatus = GuardianInvitationStatus.INVITE_GUARDIANS)
    }

    fun retrieveUserState() {
        state = state.copy(userResponse = Resource.Loading())
        viewModelScope.launch {
            val userResponse = ownerRepository.retrieveUser(enumerateGuardiansGetUserApiResponse)

            if (userResponse is Resource.Success) {
                val guardianInvitationStatus = if (userResponse.data?.ownerState != null) {
                    val ownerState : OwnerState = userResponse.data!!.ownerState as OwnerState
                    when (ownerState) {
                        is OwnerState.Ready -> GuardianInvitationStatus.READY
                        is OwnerState.GuardianSetup -> GuardianInvitationStatus.ENUMERATE_GUARDIANS
                    }
                } else {
                    GuardianInvitationStatus.ENUMERATE_GUARDIANS
                }
                state = state.copy(guardianInviteStatus = guardianInvitationStatus)
            }

            state = state.copy(userResponse = userResponse)
        }
    }

    fun updateThreshold(value: Int) {
        if (value > 0 && value <= state.createdGuardians.size) {
            state = state.copy(threshold = value)
        }
    }

    fun addGuardian() {
        if (state.createdGuardians.size == MAX_GUARDIAN_LIMIT) {
            return
        }

        viewModelScope.launch {
            state = state.copy(createGuardianResponse = Resource.Loading())

            delay(1500)

            val potentialGuardian = "Guardian ${state.createdGuardians.size + 1}"

            //Delete once full integration is worked out
            val mockGuardiansResponse: List<Guardian.ProspectGuardian> = listOf(
                Guardian.ProspectGuardian(
                    label = potentialGuardian,
                    participantId = ParticipantId(value = "BBBB"),
                    invitationId = InvitationId("AAAA"),
                    status = GuardianStatus.Initial
                )
            )

            val createGuardianApiResponse = ownerRepository.createGuardian(
                guardianName = potentialGuardian,
                mockCreatedGuardians = mockGuardiansResponse
            )

            if (createGuardianApiResponse is Resource.Success) {

                val ownerState = createGuardianApiResponse.data?.ownerState
                if (ownerState != null && ownerState is OwnerState.GuardianSetup) {
                    state = state.copy(
                        createdGuardians = ownerState.guardians,
                        threshold = state.threshold + 1
                    )
                }
            }

            state = state.copy(createGuardianResponse = createGuardianApiResponse)
        }
    }

    fun inviteGuardian(participantId: ParticipantId) {
        vaultLog(message = "Inviting guardian: ${participantId.value}")

        state = state.copy(inviteGuardianResponse = Resource.Loading())

        viewModelScope.launch {
            val inviteResponse = ownerRepository.inviteGuardian(
                participantId = participantId,
            )

            state = state.copy(inviteGuardianResponse = inviteResponse)
        }
    }

    fun resetConfirmShardReceipt() {
        state = state.copy(
            confirmShardReceiptResponse = Resource.Uninitialized,
        )
    }

    fun resetInviteResource() {
        state = state.copy(inviteGuardianResponse = Resource.Uninitialized)
    }

    fun resetUserResponse() {
        state = state.copy(userResponse = Resource.Uninitialized)
    }

    fun resetCreatePolicyResource() {
        state = state.copy(createPolicyResponse = Resource.Uninitialized)
    }

    fun resetCreateGuardianResource() {
        state = state.copy(createGuardianResponse = Resource.Uninitialized)
    }

}