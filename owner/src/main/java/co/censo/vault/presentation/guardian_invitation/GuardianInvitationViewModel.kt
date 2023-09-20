package co.censo.vault.presentation.guardian_invitation

import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.FacetecBiometry
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
    }

    fun onStart() {
        retrieveUserState()
    }

    fun onUserCreatedGuardianSet() {
        state = state.copy(guardianInviteStatus = GuardianInvitationStatus.INVITE_GUARDIANS)
    }

    fun onPolicySetupCompleted() {
        state = state.copy(guardianInviteStatus = GuardianInvitationStatus.READY)
    }

    fun onFaceScanReady(verificationId: BiometryVerificationId, facetecData: FacetecBiometry): Resource<BiometryScanResultBlob> {
        // TODO: Prepare policy request. Requires social approval VAULT-152

        return Resource.Success(BiometryScanResultBlob(""))
    }

    fun retrieveUserState() {
        state = state.copy(userResponse = Resource.Loading())
        viewModelScope.launch {
            val userResponse = ownerRepository.retrieveUser()

            if (userResponse is Resource.Success) {
                updateOwnerState(userResponse.data?.ownerState)
            }

            state = state.copy(userResponse = userResponse)
        }
    }

    private fun updateOwnerState(ownerState: OwnerState?) {
        val guardianInvitationStatus = if (ownerState != null) {
            when (ownerState) {
                is OwnerState.Ready -> GuardianInvitationStatus.READY
                is OwnerState.GuardianSetup -> GuardianInvitationStatus.ENUMERATE_GUARDIANS
            }
        } else {
            GuardianInvitationStatus.ENUMERATE_GUARDIANS
        }

        val createdGuardians = if (ownerState != null) {
            when (ownerState) {
                is OwnerState.Ready -> ownerState.policy.guardians
                is OwnerState.GuardianSetup -> ownerState.guardians
            }
        } else {
            state.createdGuardians
        }


        state = state.copy(
            createdGuardians = createdGuardians,
            ownerState = ownerState,
            guardianInviteStatus = guardianInvitationStatus
        )
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

            val createGuardianApiResponse = ownerRepository.createGuardian(
                guardianName = potentialGuardian,
            )

            if (createGuardianApiResponse is Resource.Success) {
                updateOwnerState(createGuardianApiResponse.data?.ownerState)
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

            if (inviteResponse is Resource.Success) {
                updateOwnerState(inviteResponse.data?.ownerState)
            }

            state = state.copy(inviteGuardianResponse = inviteResponse)
        }
    }

    fun enrollBiometry() {
        state = state.copy(guardianInviteStatus = GuardianInvitationStatus.CREATE_POLICY)
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