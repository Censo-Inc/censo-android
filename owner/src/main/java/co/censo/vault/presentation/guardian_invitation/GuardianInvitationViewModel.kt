package co.censo.vault.presentation.guardian_invitation

import Base58EncodedIntermediatePublicKey
import Base64EncodedData
import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.Policy
import co.censo.shared.data.model.PolicyGuardian
import co.censo.shared.data.model.Vault
import co.censo.shared.data.repository.OwnerRepository
import co.censo.vault.util.vaultLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
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

        //Mocks
        val createPolicyGetUserApiResponse = GetUserApiResponse(
            userGuid = "enim",
            biometricVerificationRequired = false,
            guardianStates = emptyList(),
            ownerState = null
        )
        val policySetupGetUserApiResponse = GetUserApiResponse(
            userGuid = "enim",
            biometricVerificationRequired = false,
            guardianStates = emptyList(),
            ownerState = OwnerState.PolicySetup(
                policy = Policy(
                    createdAt = Clock.System.now(),
                    guardians = listOf(
                        PolicyGuardian.ProspectGuardian(
                            label = "Guardian 1",
                            participantId = ParticipantId(value = "BBBB"),
                            status = GuardianStatus.Initial(Base64EncodedData("AAAA"))
                        ),
                        PolicyGuardian.ProspectGuardian(
                            label = "Guardian 2",
                            participantId = ParticipantId(value = "BBBB"),
                            status = GuardianStatus.Initial(Base64EncodedData("AAAA"))
                        ),
                        PolicyGuardian.ProspectGuardian(
                            label = "Guardian 3",
                            participantId = ParticipantId(value = "BBBB"),
                            status = GuardianStatus.Initial(Base64EncodedData("AAAA"))
                        )
                    ),
                    threshold = 3u,
                    encryptedMasterKey = Base64EncodedData(base64Encoded = "AAAA"),
                    intermediateKey = Base58EncodedIntermediatePublicKey(value = "AAAA")
                ), publicMasterEncryptionKey = Base58EncodedIntermediatePublicKey(value = "AAAA")
            )
        )
        val readyGetUserApiResponse = GetUserApiResponse(
            userGuid = "enim",
            biometricVerificationRequired = false,
            guardianStates = emptyList(),
            ownerState = OwnerState.Ready(
                policy = Policy(
                    createdAt = Clock.System.now(),
                    guardians = listOf(),
                    threshold = 3u,
                    encryptedMasterKey = Base64EncodedData(base64Encoded = "AAAA"),
                    intermediateKey = Base58EncodedIntermediatePublicKey(value = "AAAA")
                ), vault = Vault(
                    secrets = listOf(),
                    publicMasterEncryptionKey = Base58EncodedIntermediatePublicKey(value = "AAAA")
                )
            )
        )
    }

    fun onStart() {
        retrieveUserState(createPolicyGetUserApiResponse)
    }

    fun retrieveUserState(getUserApiResponse: GetUserApiResponse) {
        state = state.copy(userResponse = Resource.Loading())
        viewModelScope.launch {
            val userResponse = ownerRepository.retrieveUser(getUserApiResponse)

            if (userResponse is Resource.Success) {
                val guardianInvitationStatus = if (userResponse.data?.ownerState != null) {
                    val ownerState : OwnerState = userResponse.data!!.ownerState as OwnerState
                    when (ownerState) {
                        is OwnerState.PolicySetup -> {
                            state = state.copy(
                                potentialGuardians = ownerState.policy.guardians.map { it.participantId.value },
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
        retrieveUserState(policySetupGetUserApiResponse)
    }

    fun inviteGuardian(guardian: PolicyGuardian.ProspectGuardian) {
        vaultLog(message = "Inviting guardian: ${guardian.label}")

        state = state.copy(inviteGuardianResponse = Resource.Loading())

        viewModelScope.launch {
            val inviteResponse = ownerRepository.inviteGuardian(
                participantId = guardian.participantId,
                guardian = guardian
            )

            state = state.copy(inviteGuardianResponse = inviteResponse)
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
                    retrieveUserState(policySetupGetUserApiResponse)
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

}