package co.censo.vault.presentation.plan_setup

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
import co.censo.shared.util.CountDownTimerImpl
import co.censo.shared.util.VaultCountDownTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Base64
import javax.inject.Inject

@HiltViewModel
class PlanSetupViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository,
    private val ownerStateFlow: MutableStateFlow<Resource<OwnerState>>,
    private val verificationCodeTimer: VaultCountDownTimer,
    private val pollingVerificationTimer: VaultCountDownTimer
) : ViewModel() {

    var state by mutableStateOf(PlanSetupState())
        private set

    fun onStart() {
        retrieveOwnerState()
        // FIXME listen to the owner state updates instead of api call
        verificationCodeTimer.startCountDownTimer(CountDownTimerImpl.Companion.UPDATE_COUNTDOWN) {

            val now = Clock.System.now()
            val updatedCounter = now.epochSeconds.div(TotpGenerator.CODE_EXPIRATION)
            val time = now.toLocalDateTime(TimeZone.UTC)

            state = if (state.counter != updatedCounter) {
                state.copy(
                    currentSecond = time.second,
                    counter = updatedCounter,
                    approverCodes = generateTimeCodes(state.guardians)
                )
            } else {
                state.copy(
                    currentSecond = time.second
                )
            }
        }

        pollingVerificationTimer.startCountDownTimer(CountDownTimerImpl.Companion.POLLING_VERIFICATION_COUNTDOWN) {
            if (state.userResponse !is Resource.Loading) {
                retrieveOwnerState(silent = true)
            }
        }
    }

    fun onBackActionClick() {
        // FIXME
    }

    private fun retrieveOwnerState(silent: Boolean = false) {
        if (!silent) {
            state = state.copy(userResponse = Resource.Loading())
        }
        viewModelScope.launch {
            val ownerStateResource = ownerRepository.retrieveUser().map { it.ownerState }
            state = state.copy(
                userResponse = ownerStateResource,
            )
            updateOwnerState(ownerStateResource.data!!)
        }
    }

    private fun updateOwnerState(updatedOwnerState: OwnerState) {
        if (updatedOwnerState !is OwnerState.Ready) {
            state = state.copy(planSetupUIState = PlanSetupUIState.InviteApprovers)
            return
        }

        val guardians = updatedOwnerState.guardianSetup?.guardians ?: emptyList()

        val codes = if (state.approverCodes.isEmpty()) {
            generateTimeCodes(guardians)
        } else {
            null
        }

        if (guardians.size == 2) {
            val primaryGuardian = guardians.first { it.label != "Me" }

            state = if (primaryGuardian.status is GuardianStatus.Confirmed) {
                state.copy(
                    planSetupUIState = PlanSetupUIState.AddBackupApprover
                )
            } else {
                state.copy(
                    planSetupUIState = PlanSetupUIState.ApproverActivation
                )
            }
        }

        state = state.copy(
            approverCodes = codes ?: state.approverCodes,
            guardians = guardians,
            ownerState = updatedOwnerState,
        )

        guardians.filter {
            it.status is GuardianStatus.VerificationSubmitted
        }.forEach {
            verifyGuardian(
                it.participantId,
                it.status as GuardianStatus.VerificationSubmitted
            )
        }
    }

    fun onInvitePrimaryApprover() {
        state = state.copy(
            planSetupUIState = PlanSetupUIState.PrimaryApproverNickname
        )
    }

    fun primaryApproverNicknameChanged(nickname: String) {
        state = state.copy(
            editedNickname = nickname
        )
    }

    fun backupApproverNicknameChanged(nickname: String) {
        state = state.copy(
            editedNickname = nickname
        )
    }

    fun onSavePrimaryApprover() {
        state = state.copy(
            createPolicySetupResponse = Resource.Loading()
        )

        viewModelScope.launch {
            val participantId = ParticipantId.generate()
            val totpSecret = TotpGenerator.generateSecret()
            val encryptedTotpSecret =
                keyRepository.encryptWithDeviceKey(totpSecret.toByteArray()).base64Encoded()

            val primaryGuardian =
                (state.ownerState as OwnerState.Ready).policy.guardians.first()

            val response = ownerRepository.createPolicySetup(
                threshold = 1U,
                guardians = listOf(
                    Guardian.SetupGuardian.ImplicitlyOwner(
                        label = primaryGuardian.label,
                        participantId = ParticipantId.generate(),
                        guardianPublicKey = primaryGuardian.attributes.guardianPublicKey
                    ),
                    Guardian.SetupGuardian.ExternalApprover(
                        label = state.editedNickname,
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

    private fun generateTimeCodes(guardians: List<Guardian>): Map<ParticipantId, String> {
        val timeMillis = Clock.System.now().toEpochMilliseconds()

        return guardians
            .filter {
                it is Guardian.ProspectGuardian && (it.status.resolveDeviceEncryptedTotpSecret() != null)
            }.mapNotNull {

                when (it) {
                    is Guardian.ProspectGuardian -> {
                        val totpSecret = it.status.resolveDeviceEncryptedTotpSecret()?.base64Encoded

                        val code = TotpGenerator.generateCode(
                            secret = String(
                                keyRepository.decryptWithDeviceKey(
                                    Base64.getDecoder().decode(totpSecret)
                                )
                            ),
                            counter = timeMillis.div(TotpGenerator.CODE_EXPIRATION)
                        )

                        it.participantId to code
                    }
                    else -> null
                }
            }
            .toMap()
    }

    private fun verifyGuardian(
        participantId: ParticipantId,
        guardianStatus: GuardianStatus.VerificationSubmitted
    ) {

        val codeVerified = ownerRepository.checkCodeMatches(
            verificationCode = state.approverCodes[participantId] ?: "",
            transportKey = guardianStatus.guardianPublicKey,
            signature = guardianStatus.signature,
            timeMillis = guardianStatus.timeMillis
        )

        viewModelScope.launch {
            if (codeVerified) {

                val keyConfirmationTimeMillis = Clock.System.now().epochSeconds

                val keyConfirmationMessage =
                    guardianStatus.guardianPublicKey.getBytes() + participantId.getBytes() + keyConfirmationTimeMillis.toString()
                        .toByteArray()
                val keyConfirmationSignature =
                    keyRepository.retrieveInternalDeviceKey().sign(keyConfirmationMessage)

                val confirmGuardianShipResponse = ownerRepository.confirmGuardianShip(
                    participantId = participantId,
                    keyConfirmationSignature = keyConfirmationSignature,
                    keyConfirmationTimeMillis = keyConfirmationTimeMillis
                )

                if (confirmGuardianShipResponse is Resource.Success) {
                    updateOwnerState(confirmGuardianShipResponse.data!!.ownerState)
                }
            } else {
                val rejectVerificationResponse = ownerRepository.rejectVerification(participantId)

                if (rejectVerificationResponse is Resource.Success) {
                    updateOwnerState(rejectVerificationResponse.data!!.ownerState)
                }
            }
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
            planSetupUIState = PlanSetupUIState.ApproverActivation
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
        state =
            state.copy(navigationResource = Resource.Success(SharedScreen.OwnerVaultScreen.route))
    }

    fun reset() {
        state = PlanSetupState()
    }

}