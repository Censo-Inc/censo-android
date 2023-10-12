package co.censo.vault.presentation.activate_approvers

import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.model.CreatePolicyApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.CountDownTimerImpl.Companion.POLLING_VERIFICATION_COUNTDOWN
import co.censo.shared.util.CountDownTimerImpl.Companion.UPDATE_COUNTDOWN
import co.censo.shared.util.VaultCountDownTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Base64
import javax.inject.Inject

@HiltViewModel
class ActivateApproversViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository,
    private val verificationCodeTimer: VaultCountDownTimer,
    private val pollingVerificationTimer: VaultCountDownTimer
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

        verificationCodeTimer.startCountDownTimer(UPDATE_COUNTDOWN) {

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

        pollingVerificationTimer.startCountDownTimer(POLLING_VERIFICATION_COUNTDOWN) {
            if (state.userResponse !is Resource.Loading) {
                retrieveUserState()
            }
        }
    }

    fun onStop() {
        stopTimers()
    }

    private fun stopTimers() {
        verificationCodeTimer.stopCountDownTimer()
        pollingVerificationTimer.stopCountDownTimer()
    }

    fun createPolicy() {
        state = state.copy(createPolicyResponse = Resource.Loading())

        val ownerState = state.ownerState

        if (ownerState !is OwnerState.GuardianSetup || ownerState.threshold == null) {
            //todo throw exception here
            return
        }

        viewModelScope.launch {
            val confirmedGuardians = state.guardians
                .filterIsInstance<Guardian.ProspectGuardian>()
                .filter { it.status is GuardianStatus.Confirmed }

            val policySetupHelper = ownerRepository.getPolicySetupHelper(
                ownerState.threshold!!,
                confirmedGuardians
            )

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
        val guardians = when (ownerState) {
            is OwnerState.Ready -> ownerState.policy.guardians
            is OwnerState.GuardianSetup -> ownerState.guardians
            is OwnerState.Initial -> listOf<Guardian.ProspectGuardian>()
        }

        val codes = if (ownerState is OwnerState.GuardianSetup && state.approverCodes.isEmpty()) {
            generateTimeCodes(guardians)
        } else {
            null
        }

        state = state.copy(
            approverCodes = codes ?: state.approverCodes,
            guardians = guardians,
            ownerState = ownerState,
        )

        guardians.filter {
            it is Guardian.ProspectGuardian && it.status is GuardianStatus.VerificationSubmitted
        }.forEach {
            verifyGuardian(
                it.participantId,
                (it as Guardian.ProspectGuardian).status as GuardianStatus.VerificationSubmitted
            )
        }
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

    fun resetUserResponse() {
        state = state.copy(userResponse = Resource.Uninitialized)
    }

    fun resetCreatePolicyResource() {
        state = state.copy(createPolicyResponse = Resource.Uninitialized)
    }
}