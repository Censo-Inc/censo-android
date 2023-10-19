package co.censo.vault.presentation.plan_setup

import Base58EncodedGuardianPublicKey
import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.SharedScreen
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.cryptography.base64Encoded
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.projectLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlanSetupViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository,
    private val ownerStateFlow: MutableStateFlow<Resource<OwnerState>>
) : ViewModel() {

    var state by mutableStateOf(PlanSetupState())
        private set

    fun onStart() {
        retrieveOwnerState()
        // FIXME listen to the owner state updates instead of api call
    }

    fun onBackActionClick() {
        // FIXME
    }

    private fun retrieveOwnerState() {
        state = state.copy(ownerStateResource = Resource.Loading())
        viewModelScope.launch {
            val ownerStateResource = ownerRepository.retrieveUser().map { it.ownerState }
            state = state.copy(
                ownerStateResource = ownerStateResource,
            )
            updateOwnerState(ownerStateResource.data!!)
        }
    }

    private fun updateOwnerState(ownerState: OwnerState) {
        //todo: Should always be ready, kick out if not
        if (ownerState is OwnerState.Ready) {
            val guardians = ownerState.guardianSetup?.guardians

            if (guardians.isNullOrEmpty()) {
                state = state.copy(
                    planSetupUIState = PlanSetupUIState.InviteApprovers
                )
                return
            }

            if (guardians.size == 2) {
                //either adding backup approver or primary is in progress

                val primaryGuardian = guardians.filter { it.label != "Me" }

                projectLog(message = "Primary guardian: $primaryGuardian")
            }

        }
    }

    fun onInvitePrimaryApprover() {
        state = state.copy(
            planSetupUIState = PlanSetupUIState.PrimaryApproverNickname
        )
    }

    fun primaryApproverNicknameChanged(nickname: String) {
        state = state.copy(
            primaryApprover = state.primaryApprover.copy(
                nickname = nickname
            )
        )
    }

    fun backupApproverNicknameChanged(nickname: String) {
        state = state.copy(
            backupApprover = state.backupApprover.copy(
                nickname = nickname
            )
        )
    }

    fun onSavePrimaryApprover() {
        state = state.copy(
            createPolicySetupResponse = Resource.Loading()
        )

        viewModelScope.launch {
            val participantId = ParticipantId.generate()
            val totpSecret = TotpGenerator.generateSecret()
            val encryptedTotpSecret = keyRepository.encryptWithDeviceKey(totpSecret.toByteArray()).base64Encoded()

            state = state.copy(
                primaryApprover = state.primaryApprover.copy(
                    participantId = participantId,
                    totpSecret = totpSecret
                )
            )

            val primaryGuardian =
                (state.ownerStateResource.data!! as OwnerState.Ready).policy.guardians.first()

            val response = ownerRepository.createPolicySetup(
                threshold = 1U,
                guardians = listOf(
                    Guardian.SetupGuardian.ImplicitlyOwner(
                        label = primaryGuardian.label,
                        participantId = ParticipantId.generate(),
                        guardianPublicKey = primaryGuardian.attributes.guardianPublicKey
                    ),
                    Guardian.SetupGuardian.ExternalApprover(
                        label = state.primaryApprover.nickname,
                        participantId = participantId,
                        deviceEncryptedTotpSecret = encryptedTotpSecret
                    ),
                )
            )

            if (response is Resource.Success) {
                state = state.copy(
                    planSetupUIState = PlanSetupUIState.PrimaryApproverGettingLive
                )

                ownerStateFlow.tryEmit(response.map { it.ownerState })
            }

            state = state.copy(
                createPolicySetupResponse = response
            )
        }
    }

    fun onContinueWithBackupApprover() {
        state = state.copy(
            planSetupUIState = PlanSetupUIState.BackupApproverGettingLive
        )
    }

    fun onInviteBackupApprover() {
        state = state.copy(
            planSetupUIState = PlanSetupUIState.AddBackupApprover
        )
    }

    fun saveAndFinish() {

    }

    fun onGoLiveWithPrimaryApprover() {
        state = state.copy(
            planSetupUIState = PlanSetupUIState.PrimaryApproverActivation
        )
    }

    fun onBackupApproverVerification() {
        state = state.copy(
            // FIXME go first to the backup approver activation
            planSetupUIState = PlanSetupUIState.Completed
        )
    }

    fun resetNavigationResource() {
        state = state.copy(navigationResource = Resource.Uninitialized)
    }

    fun onFullyCompleted() {
        state = state.copy(navigationResource = Resource.Success(SharedScreen.OwnerVaultScreen.route))
    }

    fun reset() {
        state = PlanSetupState()
    }

}