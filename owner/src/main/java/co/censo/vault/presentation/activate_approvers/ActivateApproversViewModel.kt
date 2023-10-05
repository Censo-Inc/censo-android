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
import co.censo.shared.util.CountDownTimerImpl
import co.censo.shared.util.VaultCountDownTimer
import co.censo.shared.util.projectLog
import co.censo.vault.presentation.activate_approvers.ActivateApproversState.Companion.CODE_EXPIRATION
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
    private val timer: VaultCountDownTimer,
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

        timer.startCountDownTimer(CountDownTimerImpl.Companion.UPDATE_COUNTDOWN) {

            val now = Clock.System.now()
            val updatedCounter = now.epochSeconds.div(CODE_EXPIRATION)
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
    }

    fun onStop() {
        timer.stopCountDownTimer()
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
    }

    fun verifyGuardian(
        guardian: Guardian,
        guardianStatus: GuardianStatus.VerificationSubmitted
    ) {

        val codeVerified = ownerRepository.checkCodeMatches(
            verificationCode = state.approverCodes[guardian.participantId] ?: "",
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
                            counter = timeMillis.div(CODE_EXPIRATION)
                        )

                        it.participantId to code
                    }
                    else -> null
                }
            }
            .toMap()
    }

    private fun getSecondsLeftInMinute(): Long {
        val timeMillis = Clock.System.now().toEpochMilliseconds()
        val timeSeconds = timeMillis / 1000
        return timeSeconds % 60
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