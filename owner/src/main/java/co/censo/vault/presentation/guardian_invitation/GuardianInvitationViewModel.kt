package co.censo.vault.presentation.guardian_invitation

import Base64EncodedData
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.PolicyGuardian
import co.censo.shared.data.repository.OwnerRepository
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
                    val ownerState : OwnerState = userResponse.data!!.ownerState as OwnerState
                    when (ownerState) {
                        is OwnerState.PolicySetup -> {
                            val guardianDeepLinks = ownerRepository.retrieveGuardianDeepLinks(
                                ownerState.policy.guardians,
                                policyKey = ownerState.policy.intermediateKey
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

        state = state.copy(inviteGuardianResponse = Resource.Loading())

        viewModelScope.launch {
            val inviteResponse = ownerRepository.inviteGuardian(
                participantId = guardian.participantId,
                intermediatePublicKey = state.policyIntermediatePublicKey,
                guardian = guardian
            )

            state = state.copy(inviteGuardianResponse = inviteResponse)
        }
    }

    fun checkGuardianCodeMatches(
        guardian: PolicyGuardian.ProspectGuardian,
        guardianAccepted: GuardianStatus.Accepted,
        verificationCode: String
    ) {
        state = state.copy(confirmShardReceiptResponse = Resource.Uninitialized)

        val verified = ownerRepository.checkCodeMatches(
            verificationCode = verificationCode,
            transportKey = guardianAccepted.guardianTransportPublicKey,
            timeMillis = guardianAccepted.timeMillis,
            signature = guardianAccepted.signature
        )

        if (!verified) {
            state = state.copy(
                incorrectPinCode = true,
                confirmShardReceiptResponse = Resource.Uninitialized
            )
        }

        val encryptedShardWithGuardianKey = ownerRepository.encryptShardWithGuardianKey(
            deviceEncryptedShard = guardianAccepted.deviceEncryptedShard,
            transportKey = guardianAccepted.guardianTransportPublicKey
        )

        if (encryptedShardWithGuardianKey != null) {

            viewModelScope.launch {
                val confirmShardReceiptApiResponse =
                    ownerRepository.confirmShardReceipt(
                        intermediatePublicKey = state.policyIntermediatePublicKey,
                        participantId = guardian.participantId,
                        encryptedShard = Base64EncodedData(
                            Base64.getEncoder().encodeToString(encryptedShardWithGuardianKey)
                        )
                    )

                state = state.copy(confirmShardReceiptResponse = confirmShardReceiptApiResponse)
            }
        } else {
            state = state.copy(
                confirmShardReceiptResponse = Resource.Error(
                    exception = Exception("Failed to encrypt shard with guardian device key")
                )
            )
        }
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

    fun resetConfirmShardReceipt() {
        state = state.copy(
            confirmShardReceiptResponse = Resource.Uninitialized,
            incorrectPinCode = false
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

    fun resetBiometryTrigger() {
        state = state.copy(bioPromptTrigger = Resource.Uninitialized)
    }
}