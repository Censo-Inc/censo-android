package co.censo.vault.presentation.guardian_invitation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.model.CreatePolicyApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

@HiltViewModel
class ActivateApproversViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository
) : ViewModel() {

    var state by mutableStateOf(ActivateApproversState())
        private set

    companion object {
        //Do we want to limit how many guardians an owner can add?
        const val MAX_GUARDIAN_LIMIT = 5
        const val MIN_GUARDIAN_LIMIT = 3
    }

    fun onStart() {
        retrieveUserState()
    }

    fun createPolicy() {
        state = state.copy(createPolicyResponse = Resource.Loading())

        val ownerState = state.ownerState

        if (ownerState !is OwnerState.GuardianSetup || ownerState.threshold == null) {
            //todo throw exception here
            return
        }

        viewModelScope.launch {
            // TODO take only confirmed guardians. Requires social approval VAULT-152
            val policySetupHelper = ownerRepository.getPolicySetupHelper(
                ownerState.threshold!!,
                state.guardians.map { it.label })

            val createPolicyResponse: Resource<CreatePolicyApiResponse> =
                ownerRepository.createPolicy(policySetupHelper)

            if (createPolicyResponse is Resource.Success) {
                updateOwnerState(createPolicyResponse.data!!.ownerState)
            }

            state = state.copy(createPolicyResponse = createPolicyResponse)
        }
    }

    fun retrieveUserState() {
        state = state.copy(userResponse = Resource.Loading())
        viewModelScope.launch {
            val userResponse = ownerRepository.retrieveUser()

            if (userResponse is Resource.Success) {
                updateOwnerState(userResponse.data!!.ownerState)
            }

            state = state.copy(userResponse = userResponse)
        }
    }

    private fun updateOwnerState(ownerState: OwnerState) {
        val guardianInvitationStatus = when (ownerState) {
            is OwnerState.Ready -> GuardianInvitationStatus.READY
            is OwnerState.GuardianSetup, is OwnerState.Initial -> GuardianInvitationStatus.INVITE_GUARDIANS
        }

        val guardians = when (ownerState) {
            is OwnerState.Ready -> ownerState.policy.guardians
            is OwnerState.GuardianSetup -> ownerState.guardians
            is OwnerState.Initial -> listOf<Guardian.ProspectGuardian>()
        }

        state = state.copy(
            guardians = guardians,
            ownerState = ownerState,
            guardianInviteStatus = guardianInvitationStatus
        )
    }

    fun verifyGuardian(
        guardian: Guardian,
        guardianStatus: GuardianStatus.VerificationSubmitted
    ) {

        val codeVerified = ownerRepository.checkCodeMatches(
            verificationCode = "123456",
            transportKey = guardianStatus.guardianPublicKey,
            signature = guardianStatus.signature,
            timeMillis = guardianStatus.timeMillis
        )

        if (!codeVerified) {
            state = state.copy(
                codeNotValidError = true
            )
            return
        }

        viewModelScope.launch {
            val keyConfirmationTimeMillis = Clock.System.now().epochSeconds

            val keyConfirmationMessage =
                guardianStatus.guardianPublicKey.getBytes() + guardian.participantId.getBytes() + keyConfirmationTimeMillis.toString()
                    .toByteArray()
            val keyConfirmationSignature =
                keyRepository.retrieveInternalDeviceKey().sign(keyConfirmationMessage)

            val confirmGuardianShipResponse = ownerRepository.confirmGuardianShip(
                participantId = guardian.participantId,
                keyConfirmationSignature = keyConfirmationSignature,
                keyConfirmationTimeMillis = keyConfirmationTimeMillis
            )

            if (confirmGuardianShipResponse is Resource.Success) {
                updateOwnerState(confirmGuardianShipResponse.data!!.ownerState)

                state = state.copy(
                    confirmGuardianshipResponse = confirmGuardianShipResponse
                )
            }
        }
    }

    fun initPolicyCreation() {
        state = state.copy(guardianInviteStatus = GuardianInvitationStatus.CREATE_POLICY)
    }

    fun resetInvalidCode() {
        state = state.copy(codeNotValidError = false)
    }

    fun resetConfirmGuardianshipResponse() {
        state = state.copy(
            confirmGuardianshipResponse = Resource.Uninitialized,
        )
    }

    fun resetUserResponse() {
        state = state.copy(userResponse = Resource.Uninitialized)
    }

    fun resetCreatePolicyResource() {
        state = state.copy(createPolicyResponse = Resource.Uninitialized)
    }
}