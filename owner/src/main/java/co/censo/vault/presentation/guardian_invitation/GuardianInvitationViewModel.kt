package co.censo.vault.presentation.guardian_invitation

import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.generateHexString
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.CreatePolicyApiResponse
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.projectLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

@HiltViewModel
class GuardianInvitationViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository
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
        state = state.copy(guardianInviteStatus = GuardianInvitationStatus.CREATE_POLICY_SETUP)
    }

    suspend fun onPolicySetupCreationFaceScanReady(verificationId: BiometryVerificationId, facetecData: FacetecBiometry): Resource<BiometryScanResultBlob> {
        state = state.copy(createPolicySetupResponse = Resource.Loading())

        return viewModelScope.async {
            val createPolicySetupResponse = ownerRepository.createPolicySetup(
                state.threshold,
                state.guardians.map { Guardian.SetupGuardian(it.label, it.participantId) },
                verificationId,
                facetecData
            )

            if (createPolicySetupResponse is Resource.Success) {
                updateOwnerState(createPolicySetupResponse.data?.ownerState)
                state = state.copy(guardianInviteStatus = GuardianInvitationStatus.INVITE_GUARDIANS)
            }

            state = state.copy(createPolicySetupResponse = createPolicySetupResponse)

            createPolicySetupResponse.map { it.scanResultBlob }
        }.await()
    }

    fun createPolicy() {
        state = state.copy(createPolicyResponse = Resource.Loading())

        viewModelScope.launch {
            // TODO take only confirmed guardians. Requires social approval VAULT-152
            val policySetupHelper = ownerRepository.getPolicySetupHelper(state.threshold, state.guardians.map { it.label })

            val createPolicyResponse: Resource<CreatePolicyApiResponse> = ownerRepository.createPolicy(policySetupHelper)

            if (createPolicyResponse is Resource.Success) {
                updateOwnerState(createPolicyResponse.data?.ownerState)
            }

            state = state.copy(createPolicyResponse = createPolicyResponse)
        }
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
                is OwnerState.GuardianSetup -> GuardianInvitationStatus.INVITE_GUARDIANS
                is OwnerState.Initial -> GuardianInvitationStatus.ENUMERATE_GUARDIANS
            }
        } else {
            GuardianInvitationStatus.ENUMERATE_GUARDIANS
        }

        val guardians = if (ownerState != null) {
            when (ownerState) {
                is OwnerState.Ready -> ownerState.policy.guardians
                is OwnerState.GuardianSetup -> ownerState.guardians
                is OwnerState.Initial -> listOf()
            }
        } else {
            state.guardians
        }


        state = state.copy(
            guardians = guardians,
            ownerState = ownerState,
            guardianInviteStatus = guardianInvitationStatus
        )
    }

    fun updateThreshold(value: UInt) {
        if (value <= state.guardians.size.toUInt()) {
            state = state.copy(threshold = value)
        }
    }

    fun addGuardian() {
        if (state.guardians.size == MAX_GUARDIAN_LIMIT) {
            return
        }

        state = state.copy(guardians = state.guardians + Guardian.SetupGuardian(
            label = "Guardian ${state.guardians.size + 1}",
            participantId = ParticipantId(generateHexString())
        ))
    }

    fun inviteGuardian(participantId: ParticipantId) {
        projectLog(message = "Inviting guardian: ${participantId.value}")

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
                updateOwnerState(confirmGuardianShipResponse.data?.ownerState)

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

    fun resetInviteResource() {
        state = state.copy(inviteGuardianResponse = Resource.Uninitialized)
    }

    fun resetUserResponse() {
        state = state.copy(userResponse = Resource.Uninitialized)
    }

    fun resetCreatePolicySetupResource() {
        state = state.copy(createPolicySetupResponse = Resource.Uninitialized)
    }

    fun resetCreatePolicyResource() {
        state = state.copy(createPolicyResponse = Resource.Uninitialized)
    }
}