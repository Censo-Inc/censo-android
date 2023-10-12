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

    companion object {
        const val VALID_CODE_LENGTH = 6
    }

    var state by mutableStateOf(GuardianHomeState())
        private set

    fun onStart() {
        retrieveUserState()

        userStatePollingTimer.startCountDownTimer(POLLING_VERIFICATION_COUNTDOWN) {
            if (state.userResponse !is Resource.Loading) {
                retrieveUserState()
            }
        }
    }

    fun onStop() {
        userStatePollingTimer.stopCountDownTimer()
        stopRecoveryTotpGeneration()
    }

    private fun determineGuardianUIState(
        guardianState: GuardianState?,
    ) {
        viewModelScope.launch {
            val inviteCode = state.invitationId.value.ifEmpty { guardianRepository.retrieveInvitationId() }
            val userSavedPrivateKey = guardianRepository.userHasKeySavedInCloud()

            val guardianUIState = if (guardianState == null) {
                if (guardianRepository.retrieveParticipantId().isEmpty()) {
                    GuardianUIState.INVALID_PARTICIPANT_ID
                } else if (inviteCode.isEmpty()) {
                    GuardianUIState.MISSING_INVITE_CODE
                } else {
                    GuardianUIState.INVITE_READY
                }
            } else {
                when (val phase = guardianState.phase) {
                    is GuardianPhase.WaitingForCode -> {
                        if (!userSavedPrivateKey) {
                            GuardianUIState.NEED_SAVE_KEY
                        } else {
                            GuardianUIState.WAITING_FOR_CODE
                        }
                    }
                    is GuardianPhase.Complete -> GuardianUIState.COMPLETE
                    is GuardianPhase.WaitingForVerification -> GuardianUIState.WAITING_FOR_CONFIRMATION
                    is GuardianPhase.VerificationRejected -> GuardianUIState.CODE_REJECTED
                    is GuardianPhase.RecoveryRequested -> GuardianUIState.RECOVERY_REQUESTED
                    is GuardianPhase.RecoveryVerification -> GuardianUIState.RECOVERY_WAITING_FOR_TOTP_FROM_OWNER
                    is GuardianPhase.RecoveryConfirmation -> {
                        confirmOrRejectOwner(guardianState.participantId, phase)
                        GuardianUIState.RECOVERY_VERIFYING_TOTP_FROM_OWNER
                    }
                }
            }

            state = state.copy(
                guardianUIState = guardianUIState,
                guardianState = guardianState ?: state.guardianState,
                invitationId = InvitationId(inviteCode),
                participantId = guardianState?.participantId?.value ?: state.participantId
            )

            val guardianPhase = guardianState?.phase
            if (guardianPhase is GuardianPhase.RecoveryVerification && state.recoveryTotp != null) {
                startRecoveryTotpGeneration(guardianPhase.encryptedTotpSecret)
            }
        }
    }

    fun createGuardianKey() {
        viewModelScope.launch {
            val guardianEncryptionKey = keyRepository.createGuardianKey()
            keyRepository.saveKeyInCloud(
                Base58EncodedPrivateKey(
                    Base58.base58Encode(
                        guardianEncryptionKey.privateKeyRaw()
                    )
                )
            )
            state = state.copy(
                guardianEncryptionKey = guardianEncryptionKey
            )
            determineGuardianUIState(state.guardianState)
        }
    }

    private fun retrieveUserState() {
        state = state.copy(userResponse = Resource.Loading())
        viewModelScope.launch {
            val userResponse = ownerRepository.retrieveUser()

            state = if (userResponse is Resource.Success) {
                val participantId = guardianRepository.retrieveParticipantId()

                val guardianState = if (participantId.isEmpty()) {
                    userResponse.data?.guardianStates?.firstOrNull()
                } else {
                    userResponse.data?.guardianStates?.firstOrNull { it.participantId.value == participantId }
                }
                determineGuardianUIState(guardianState)
                projectLog(message = "User Response: ${userResponse.data}")
                state.copy(
                    userResponse = userResponse,
                )
            } else {
                state.copy(userResponse = userResponse)
            }
        }
    }

    fun acceptGuardianship() {
        state = state.copy(acceptGuardianResource = Resource.Loading())

        viewModelScope.launch {
            val acceptResource = guardianRepository.acceptGuardianship(
                invitationId = state.invitationId,
            )

            state = if (acceptResource is Resource.Success) {
                determineGuardianUIState(acceptResource.data?.guardianState)
                state.copy(
                    acceptGuardianResource = acceptResource
                )
            } else {
                state.copy(acceptGuardianResource = acceptResource)
            }
        }
    }

    fun declineGuardianship() {
        state = state.copy(declineGuardianResource = Resource.Loading())

        viewModelScope.launch {
            val declineResource = guardianRepository.declineGuardianship(
                invitationId = state.invitationId,
            )

            state = if (declineResource is Resource.Success) {
                state.copy(
                    declineGuardianResource = declineResource
                )
            } else {
                state.copy(declineGuardianResource = declineResource)
            }
        }
    }

    fun updateVerificationCode(value: String) {
        if (value.isDigitsOnly()) {
            state = state.copy(verificationCode = value)
            if (state.verificationCode.length == VALID_CODE_LENGTH) {
                submitVerificationCode()
            }
        }

        if (state.submitVerificationResource is Resource.Error) {
            state = state.copy(submitVerificationResource = Resource.Uninitialized)
        }
    }

    private fun submitVerificationCode() {
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

            //todo: Have user input this
            val submitVerificationResource = guardianRepository.submitGuardianVerification(
                invitationId = state.invitationId.value,
                submitGuardianVerificationRequest = signedVerificationData
            )

            state = if (submitVerificationResource is Resource.Success) {
                determineGuardianUIState(submitVerificationResource.data?.guardianState)
                state.copy(
                    submitVerificationResource = submitVerificationResource
                )
            } else {
                state.copy(submitVerificationResource = submitVerificationResource, verificationCode = "")
            }
        }
    }

    private suspend fun loadPrivateKeyFromCloud() {
        val privateKeyFromCloud = keyRepository.retrieveKeyFromCloud()

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

    private fun generateRecoveryTotp(encryptedTotpSecret: Base64EncodedData): GuardianHomeState.RecoveryTotpState {
        val now = Clock.System.now()
        val counter = now.epochSeconds.div(TotpGenerator.CODE_EXPIRATION)

        return GuardianHomeState.RecoveryTotpState(
            code = TotpGenerator.generateCode(
                secret = String(keyRepository.decryptWithDeviceKey(encryptedTotpSecret.bytes)),
                counter = counter,
            ),
            counter = counter,
            currentSecond = now.toLocalDateTime(TimeZone.UTC).second,
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
        val totpIsCorrect = guardianRepository.checkTotpMatches(
            state.recoveryTotp?.code ?: "",
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
                if (response is Resource.Success) {
                    determineGuardianUIState(response.data?.guardianStates?.forParticipant(state.participantId))
                }

                stopRecoveryTotpGeneration()
                state = state.copy(approveRecoveryResource = response)
            } else {
                val response = guardianRepository.rejectRecovery(participantId)
                if (response is Resource.Success) {
                    determineGuardianUIState(response.data?.guardianStates?.forParticipant(state.participantId))
                }
                state = state.copy(rejectRecoveryResource = response)
            }
        }
    }
}