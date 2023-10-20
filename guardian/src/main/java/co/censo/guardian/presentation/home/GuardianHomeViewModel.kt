package co.censo.guardian.presentation.home

import Base58EncodedPrivateKey
import Base64EncodedData
import InvitationId
import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.cryptography.base64Encoded
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.cryptography.key.ExternalEncryptionKey
import co.censo.shared.data.model.GuardianPhase
import co.censo.shared.data.model.GuardianState
import co.censo.shared.data.model.forParticipant
import co.censo.shared.data.repository.GuardianRepository
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.CountDownTimerImpl.Companion.POLLING_VERIFICATION_COUNTDOWN
import co.censo.shared.util.CountDownTimerImpl.Companion.UPDATE_COUNTDOWN
import co.censo.shared.util.VaultCountDownTimer
import co.censo.shared.util.projectLog
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.novacrypto.base58.Base58
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigInteger
import javax.inject.Inject

@HiltViewModel
class GuardianHomeViewModel @Inject constructor(
    private val guardianRepository: GuardianRepository,
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository,
    private val recoveryTotpTimer: VaultCountDownTimer,
    private val userStatePollingTimer: VaultCountDownTimer
) : ViewModel() {

    var state by mutableStateOf(GuardianHomeState())
        private set

    fun onStart() {
        retrieveApproverState()

        userStatePollingTimer.startCountDownTimer(POLLING_VERIFICATION_COUNTDOWN) {
            if (state.userResponse !is Resource.Loading) {
                silentRetrieveApproverState()
            }
        }
    }

    fun onStop() {
        userStatePollingTimer.stopCountDownTimer()
        stopRecoveryTotpGeneration()
    }

    fun retrieveApproverState() {
        state = state.copy(userResponse = Resource.Loading())

        silentRetrieveApproverState()
    }

    private fun silentRetrieveApproverState() {
        viewModelScope.launch {
            val userResponse = ownerRepository.retrieveUser()

            if (userResponse is Resource.Success) {
                val participantId = guardianRepository.retrieveParticipantId()
                val guardianStates = userResponse.data!!.guardianStates

                if (participantId.isEmpty()) {
                    determineGuardianUIState(guardianStates.firstOrNull())
                } else {
                    when (val guardianState = guardianStates.forParticipant(participantId)) {
                        null -> {
                            guardianRepository.clearParticipantId()
                            state = state.copy(guardianUIState = GuardianUIState.INVALID_PARTICIPANT_ID)
                        }
                        else -> determineGuardianUIState(guardianState)
                    }
                }
            }

            projectLog(message = "User Response: ${userResponse.data}")

            state = state.copy(userResponse = userResponse)
        }
    }

    private fun determineGuardianUIState(
        guardianState: GuardianState?,
    ) {
        viewModelScope.launch {
            val inviteCode = state.invitationId.value.ifEmpty { guardianRepository.retrieveInvitationId() }
            val participantId = guardianRepository.retrieveParticipantId()

            val guardianUIStateNewNew = if (guardianState == null) { // new onboarding
                if (inviteCode.isEmpty()) {
                    GuardianUIState.MISSING_INVITE_CODE
                } else {
                    GuardianUIState.INVITE_READY
                }
            } else {
                when (val phase = guardianState.phase) {            // existing approver

                    // Onboarding, invitationId is mandatory
                    is GuardianPhase.WaitingForCode -> if (inviteCode.isEmpty()) GuardianUIState.MISSING_INVITE_CODE else GuardianUIState.WAITING_FOR_CODE
                    is GuardianPhase.WaitingForVerification -> if (inviteCode.isEmpty()) GuardianUIState.MISSING_INVITE_CODE else GuardianUIState.WAITING_FOR_CONFIRMATION
                    is GuardianPhase.VerificationRejected -> if (inviteCode.isEmpty()) GuardianUIState.MISSING_INVITE_CODE else GuardianUIState.CODE_REJECTED

                    // No action needed
                    is GuardianPhase.Complete -> {
                        when (state.approveRecoveryResource) {
                            is Resource.Success -> GuardianUIState.ACCESS_APPROVED
                            else -> GuardianUIState.COMPLETE
                        }
                    }

                    // recovery, participantId is mandatory
                    is GuardianPhase.RecoveryRequested -> if (participantId.isEmpty()) GuardianUIState.COMPLETE else GuardianUIState.ACCESS_REQUESTED
                    is GuardianPhase.RecoveryVerification -> if (participantId.isEmpty()) GuardianUIState.COMPLETE else GuardianUIState.ACCESS_WAITING_FOR_TOTP_FROM_OWNER
                    is GuardianPhase.RecoveryConfirmation -> if (participantId.isEmpty()) GuardianUIState.COMPLETE else {
                        confirmOrRejectOwner(guardianState.participantId, phase)
                        GuardianUIState.ACCESS_VERIFYING_TOTP_FROM_OWNER
                    }
                }
            }

            state = state.copy(
                guardianUIState = guardianUIStateNewNew,
                guardianState = guardianState ?: state.guardianState,
                invitationId = InvitationId(inviteCode),
                participantId = guardianState?.participantId?.value ?: state.participantId
            )

            val guardianPhase = guardianState?.phase
            if (guardianPhase is GuardianPhase.RecoveryVerification) {
                startRecoveryTotpGeneration(guardianPhase.encryptedTotpSecret)
            }
        }
    }

    fun acceptGuardianship() {
        state = state.copy(acceptGuardianResource = Resource.Loading())

        viewModelScope.launch {
            val acceptResource = guardianRepository.acceptGuardianship(
                invitationId = state.invitationId,
            )

            if (acceptResource is Resource.Success) {
                val participantId = acceptResource.data!!.guardianState.participantId
                state = state.copy(participantId = participantId.value)

                if (!keyRepository.userHasKeySavedInCloud(participantId)) {
                    createAndSaveGuardianKey(participantId)
                }
                determineGuardianUIState(acceptResource.data?.guardianState)
            }

            state = state.copy(acceptGuardianResource = acceptResource)
        }
    }

    private suspend fun createAndSaveGuardianKey(participantId: ParticipantId) {
        val guardianEncryptionKey = keyRepository.createGuardianKey()
        keyRepository.saveKeyInCloud(
            Base58EncodedPrivateKey(
                Base58.base58Encode(
                    guardianEncryptionKey.privateKeyRaw()
                )
            ),
            participantId = participantId
        )
        state = state.copy(
            guardianEncryptionKey = guardianEncryptionKey
        )
    }

    fun updateVerificationCode(value: String) {
        if (state.submitVerificationResource is Resource.Error) {
            state = state.copy(submitVerificationResource = Resource.Uninitialized)
        }

        if (value.isDigitsOnly()) {
            state = state.copy(verificationCode = value)

            if (state.verificationCode.length == TotpGenerator.CODE_LENGTH) {
                submitVerificationCode()
            }
        }
    }

    fun submitVerificationCode() {
        state = state.copy(submitVerificationResource = Resource.Loading())

        viewModelScope.launch {

            if (state.guardianEncryptionKey == null) {
                loadPrivateKeyFromCloud()
            }

            if (state.invitationId.value.isEmpty()) {
                loadInvitationId()
            }

            val signedVerificationData = guardianRepository.signVerificationCode(
                verificationCode = state.verificationCode,
                state.guardianEncryptionKey!!
            )

            val submitVerificationResource = guardianRepository.submitGuardianVerification(
                invitationId = state.invitationId.value,
                submitGuardianVerificationRequest = signedVerificationData
            )

            state = if (submitVerificationResource is Resource.Success) {
                determineGuardianUIState(submitVerificationResource.data?.guardianState)
                state.copy(
                    submitVerificationResource = submitVerificationResource,
                    verificationCode = ""
                )
            } else {
                state.copy(submitVerificationResource = submitVerificationResource)
            }
        }
    }

    private suspend fun loadPrivateKeyFromCloud() {
        val privateKeyFromCloud =
            keyRepository.retrieveKeyFromCloud(participantId = ParticipantId(state.participantId))

        val privateKeyRaw = Base58.base58Decode(privateKeyFromCloud.value)

        val recreatedEncryptionKey =
            EncryptionKey.generateFromPrivateKeyRaw(BigInteger(privateKeyRaw))

        state = state.copy(guardianEncryptionKey = recreatedEncryptionKey)
    }

    private fun loadInvitationId() {
        state = state.copy(
            invitationId = when (val guardianState = state.guardianState?.phase) {
                is GuardianPhase.WaitingForCode -> guardianState.invitationId
                is GuardianPhase.VerificationRejected -> guardianState.invitationId
                else -> {
                    InvitationId(guardianRepository.retrieveInvitationId())
                }
            }
        )
    }

    fun storeRecoveryTotpSecret() {
        state = state.copy(storeRecoveryTotpSecretResource = Resource.Loading())

        viewModelScope.launch {
            val secret = TotpGenerator.generateSecret()
            val encryptedSecret = keyRepository
                .encryptWithDeviceKey(secret.toByteArray())
                .base64Encoded()

            val submitRecoveryTotpSecretResource = guardianRepository.storeRecoveryTotpSecret(state.participantId, encryptedSecret)

            if (submitRecoveryTotpSecretResource is Resource.Success) {
                determineGuardianUIState(submitRecoveryTotpSecretResource.data?.guardianStates?.forParticipant(state.participantId))
                startRecoveryTotpGeneration(encryptedSecret)
            }

            state = state.copy(
                storeRecoveryTotpSecretResource = submitRecoveryTotpSecretResource
            )
        }
    }

    private fun generateRecoveryTotp(encryptedTotpSecret: Base64EncodedData, instant: Instant = Clock.System.now()): GuardianHomeState.RecoveryTotpState {
        val counter = instant.epochSeconds.div(TotpGenerator.CODE_EXPIRATION)

        return GuardianHomeState.RecoveryTotpState(
            code = TotpGenerator.generateCode(
                secret = String(keyRepository.decryptWithDeviceKey(encryptedTotpSecret.bytes)),
                counter = counter,
            ),
            counter = counter,
            currentSecond = instant.toLocalDateTime(TimeZone.UTC).second,
            encryptedSecret = encryptedTotpSecret
        )
    }

    private fun startRecoveryTotpGeneration(encryptedSecret: Base64EncodedData) {
        state = state.copy(recoveryTotp = generateRecoveryTotp(encryptedSecret))
        recoveryTotpTimer.startCountDownTimer(UPDATE_COUNTDOWN) {
            state.recoveryTotp?.also { totp ->
                val now = Clock.System.now()

                state = if (totp.counter != now.epochSeconds.div(TotpGenerator.CODE_EXPIRATION)) {
                    state.copy(
                        recoveryTotp = generateRecoveryTotp(totp.encryptedSecret)
                    )
                } else {
                    state.copy(
                        recoveryTotp = totp.copy(
                            currentSecond = now.toLocalDateTime(TimeZone.UTC).second
                        )
                    )
                }
            }
        }
    }

    private fun stopRecoveryTotpGeneration() {
        recoveryTotpTimer.stopCountDownTimer()
        state = state.copy(recoveryTotp = null)
    }

    private fun confirmOrRejectOwner(participantId: ParticipantId, recoveryConfirmation: GuardianPhase.RecoveryConfirmation) {
        val recoveryTotp = generateRecoveryTotp(recoveryConfirmation.encryptedTotpSecret, Instant.fromEpochMilliseconds(recoveryConfirmation.ownerKeySignatureTimeMillis))

        val totpIsCorrect = guardianRepository.checkTotpMatches(
            recoveryTotp.code,
            recoveryConfirmation.ownerPublicKey,
            signature = recoveryConfirmation.ownerKeySignature,
            timeMillis = recoveryConfirmation.ownerKeySignatureTimeMillis
        )

        viewModelScope.launch {
            if (totpIsCorrect) {
                if (state.guardianEncryptionKey == null) {
                    loadPrivateKeyFromCloud()
                }

                val ownerPublicKey = ExternalEncryptionKey.generateFromPublicKeyBase58(recoveryConfirmation.ownerPublicKey)
                val response = guardianRepository.approveRecovery(
                    participantId,
                    encryptedShard = ownerPublicKey.encrypt(
                        state.guardianEncryptionKey!!.decrypt(recoveryConfirmation.guardianEncryptedShard.bytes)
                    ).base64Encoded()
                )

                state = state.copy(approveRecoveryResource = response)

                if (response is Resource.Success) {
                    guardianRepository.clearParticipantId()
                    determineGuardianUIState(response.data?.guardianStates?.forParticipant(state.participantId))
                }

                stopRecoveryTotpGeneration()
            } else {
                val response = guardianRepository.rejectRecovery(participantId)
                if (response is Resource.Success) {
                    determineGuardianUIState(response.data?.guardianStates?.forParticipant(state.participantId))
                }
                state = state.copy(rejectRecoveryResource = response)
            }
        }
    }

    fun showCloseConfirmationDialog() {
        state = state.copy(
            showTopBarCancelConfirmationDialog = true
        )
    }

    fun hideCloseConfirmationDialog() {
        state = state.copy(
            showTopBarCancelConfirmationDialog = false
        )
    }

    fun onTopBarCloseConfirmed() {
        hideCloseConfirmationDialog()

        when (state.guardianUIState) {
            // onboarding
            GuardianUIState.INVITE_READY,
            GuardianUIState.WAITING_FOR_CODE,
            GuardianUIState.WAITING_FOR_CONFIRMATION,
            GuardianUIState.CODE_REJECTED -> cancelOnboarding()

            // recovery
            GuardianUIState.INVALID_PARTICIPANT_ID,
            GuardianUIState.ACCESS_REQUESTED,
            GuardianUIState.ACCESS_WAITING_FOR_TOTP_FROM_OWNER,
            GuardianUIState.ACCESS_VERIFYING_TOTP_FROM_OWNER -> cancelRecovery()

            // no "Close" button for these states
            GuardianUIState.MISSING_INVITE_CODE,
            GuardianUIState.COMPLETE,
            GuardianUIState.ACCESS_APPROVED -> {
            }
        }
    }

    private fun cancelRecovery() {
        guardianRepository.clearParticipantId()

        state = state.copy(
            participantId = "",
            guardianUIState = GuardianUIState.COMPLETE
        )
    }

    fun cancelOnboarding() {
        guardianRepository.clearInvitationId()

        state = state.copy(
            invitationId = InvitationId(""),
            guardianUIState = GuardianUIState.MISSING_INVITE_CODE
        )
    }

    fun resetAcceptGuardianResource() {
        state = state.copy(
            acceptGuardianResource = Resource.Uninitialized
        )
    }

    fun resetSubmitVerificationResource() {
        state = state.copy(
            submitVerificationResource = Resource.Uninitialized
        )
    }

    fun resetStoreRecoveryTotpSecretResource() {
        state = state.copy(
            storeRecoveryTotpSecretResource = Resource.Uninitialized
        )
    }

    fun resetApproveRecoveryResource() {
        state = state.copy(approveRecoveryResource = Resource.Uninitialized)
    }

    fun resetRejectRecoveryResource() {
        state = state.copy(rejectRecoveryResource = Resource.Uninitialized)
    }
}