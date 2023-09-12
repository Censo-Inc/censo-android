package co.censo.vault.presentation.guardian_invitation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.vault.data.Resource
import co.censo.vault.data.cryptography.CryptographyManager
import co.censo.vault.data.cryptography.CryptographyManagerImpl
import co.censo.vault.data.cryptography.ECIESManager
import co.censo.vault.data.model.Guardian
import co.censo.vault.data.model.GuardianStatus
import co.censo.vault.data.model.OwnerState
import co.censo.vault.data.model.PolicyGuardian
import co.censo.vault.data.repository.OwnerRepository
import co.censo.vault.util.vaultLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Base64
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
                    val ownerState = userResponse.data.ownerState
                    when (ownerState) {
                        is OwnerState.PolicySetup -> {
                            val guardianDeepLinks = ownerRepository.retrieveGuardianDeepLinks(
                                ownerState.policy.guardians, policyKey = ownerState.policy.intermediateKey
                            )

                            state = state.copy(
                                potentialGuardians = ownerState.policy.guardians.map { it.participantId.value },
                                guardianDeepLinks = guardianDeepLinks,
                                prospectGuardians = ownerState.policy.guardians,
                                policyIntermediatePublicKey = ownerState.policy.intermediateKey
                            )
                            GuardianInvitationStatus.POLICY_SETUP
                        }
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

    fun inviteGuardian(guardian: PolicyGuardian.ProspectGuardian) {
        vaultLog(message = "Inviting guardian: ${guardian.label}")

        state = state.copy(inviteGuardian = Resource.Loading())

        viewModelScope.launch {
            val inviteResponse = ownerRepository.inviteGuardian(
                participantId = guardian.participantId,
                intermediatePublicKey = state.policyIntermediatePublicKey,
                guardian = guardian
            )

            state = state.copy(inviteGuardian = inviteResponse)
        }
    }

    fun checkGuardianCodeMatches(
        guardianAccepted: GuardianStatus.Accepted,
        verificationCode: String
    ) {
        val verified = ownerRepository.checkCodeMatches(
            verificationCode = verificationCode,
            transportKey = guardianAccepted.guardianTransportPublicKey,
            timeMillis = guardianAccepted.timeMillis,
            signature = guardianAccepted.signature
        )

        if (!verified) {
            //todo: show error to user...
        }

        val encryptedShardWithGuardianKey = ownerRepository.encryptShardWithGuardianKey(
            deviceEncryptedShard = guardianAccepted.deviceEncryptedShard,
            transportKey = guardianAccepted.guardianTransportPublicKey
        )

        if (encryptedShardWithGuardianKey != null) {
            //todo send to API
        } else {
            //todo: show error to user
        }

        //Todo: Add API call
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

    fun resetInviteResource() {
        state = state.copy(inviteGuardian = Resource.Uninitialized)
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
}