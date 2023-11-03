package co.censo.guardian.presentation.home

import Base58EncodedPrivateKey
import Base64EncodedData
import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.guardian.data.ApproverAccessUIState
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
import co.censo.shared.getInviteCodeFromDeeplink
import co.censo.shared.presentation.cloud_storage.CloudStorageActionData
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.util.CountDownTimerImpl.Companion.POLLING_VERIFICATION_COUNTDOWN
import co.censo.shared.util.CountDownTimerImpl.Companion.UPDATE_COUNTDOWN
import co.censo.shared.util.VaultCountDownTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@HiltViewModel
class ApproverAccessViewModel @Inject constructor(
    private val guardianRepository: GuardianRepository,
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository,
    private val recoveryTotpTimer: VaultCountDownTimer,
    private val userStatePollingTimer: VaultCountDownTimer
) : ViewModel() {

    var state by mutableStateOf(ApproverAccessState())
        private set

    fun onStart() {
        retrieveApproverState(false)

        userStatePollingTimer.startCountDownTimer(POLLING_VERIFICATION_COUNTDOWN) {
            if (state.shouldCheckRecoveryCode) {
                retrieveApproverState(true)
            }
        }
    }

    fun onStop() {
        userStatePollingTimer.stopCountDownTimer()
        stopRecoveryTotpGeneration()
    }

    fun retrieveApproverState(silently: Boolean) {
        if (!silently) {
            state = state.copy(userResponse = Resource.Loading())
        }

        viewModelScope.launch {
            val userResponse = ownerRepository.retrieveUser()

            state = state.copy(userResponse = userResponse)

            if (userResponse is Resource.Success) {
                val guardianStates = userResponse.data!!.guardianStates
                checkApproverHasParticipantData(guardianStates)
            }
        }
    }

    private fun checkApproverHasParticipantData(guardianStates: List<GuardianState>) {
        val participantId = guardianRepository.retrieveParticipantId()

        //If participantId is empty then the approver is in the complete state
        if (participantId.isEmpty()) {
            state = state.copy(approverAccessUIState = ApproverAccessUIState.UserNeedsPasteRecoveryLink)
            return
        }

        when (val guardianState = guardianStates.forParticipant(participantId = participantId)) {
            null -> {
                guardianRepository.clearParticipantId()
                state = state.copy(approverAccessUIState = ApproverAccessUIState.UserNeedsPasteRecoveryLink)
            }
            else -> {
                assignGuardianStateAndParticipantId(
                    guardianState = guardianState,
                    participantId = participantId
                )
                determineApproverAccessUIState(guardianState)
            }
        }
    }

    private fun assignGuardianStateAndParticipantId(guardianState: GuardianState?, participantId: String) {
        state = state.copy(participantId = participantId, guardianState = guardianState)
    }

    private fun assignParticipantId(participantId: String) {
        state = state.copy(participantId = participantId)
    }

    private fun determineApproverAccessUIState(guardianState: GuardianState) {
        state = when (val phase = guardianState.phase) {
            is GuardianPhase.Complete -> {
                when (state.approveRecoveryResource) {
                    is Resource.Success -> state.copy(approverAccessUIState = ApproverAccessUIState.AccessApproved)
                    else -> state.copy(approverAccessUIState = ApproverAccessUIState.Complete)
                }
            }

            is GuardianPhase.RecoveryRequested ->
                state.copy(approverAccessUIState = ApproverAccessUIState.AccessRequested)
            is GuardianPhase.RecoveryVerification -> {
                startRecoveryTotpGeneration(phase.encryptedTotpSecret)
                state.copy(approverAccessUIState = ApproverAccessUIState.WaitingForToTPFromOwner)
            }
            is GuardianPhase.RecoveryConfirmation -> {
                loadPrivateKeyFromCloud(phase)
                state.copy(approverAccessUIState = ApproverAccessUIState.VerifyingToTPFromOwner)
            }

            else -> state
        }
    }

    private fun loadPrivateKeyFromCloud(recoveryPhase: GuardianPhase.RecoveryConfirmation) {
        state = state.copy(
            cloudStorageAction = CloudStorageActionData(
                triggerAction = true, action = CloudStorageActions.DOWNLOAD
            ),
            recoveryConfirmationPhase = recoveryPhase,
            loadKeyFromCloudResource = Resource.Loading()
        )
    }

    private fun confirmOrRejectOwnerAccessRequest(
        participantId: ParticipantId, recoveryConfirmation: GuardianPhase.RecoveryConfirmation
    ) {
        val recoveryTotp = generateRecoveryTotp(
            recoveryConfirmation.encryptedTotpSecret,
            Instant.fromEpochMilliseconds(recoveryConfirmation.ownerKeySignatureTimeMillis)
        )

        val totpIsCorrect = guardianRepository.checkTotpMatches(
            recoveryTotp.encryptedSecret,
            recoveryConfirmation.ownerPublicKey,
            signature = recoveryConfirmation.ownerKeySignature,
            timeMillis = recoveryConfirmation.ownerKeySignatureTimeMillis
        )

        viewModelScope.launch(Dispatchers.IO) {
            if (totpIsCorrect) {
                state = state.copy(approveRecoveryResource = Resource.Loading())

                if (state.guardianEncryptionKey == null) {
                    state = state.copy(approveRecoveryResource = Resource.Uninitialized)
                    loadPrivateKeyFromCloud(recoveryPhase = recoveryConfirmation)
                    return@launch
                }

                val ownerPublicKey = ExternalEncryptionKey.generateFromPublicKeyBase58(recoveryConfirmation.ownerPublicKey)
                val response = guardianRepository.approveRecovery(
                    participantId,
                    encryptedShard = ownerPublicKey.encrypt(
                        state.guardianEncryptionKey!!.decrypt(recoveryConfirmation.guardianEncryptedShard.bytes)
                    ).base64Encoded()
                )

                state = state.copy(approveRecoveryResource = response, ownerEnteredWrongCode = false)

                if (response is Resource.Success) {
                    guardianRepository.clearParticipantId()
                    determineApproverAccessUIState(response.data?.guardianStates?.forParticipant(state.participantId)!!)
                }

                stopRecoveryTotpGeneration()
            } else {
                state = state.copy(rejectRecoveryResource = Resource.Loading())
                val response = guardianRepository.rejectRecovery(participantId)
                state = state.copy(rejectRecoveryResource = response, ownerEnteredWrongCode = true)
                if (response is Resource.Success) {
                    determineApproverAccessUIState(response.data?.guardianStates?.forParticipant(state.participantId)!!)
                }
            }
        }
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
                determineApproverAccessUIState(
                    submitRecoveryTotpSecretResource.data?.guardianStates?.forParticipant(
                        state.participantId
                    )!!
                )
                startRecoveryTotpGeneration(encryptedSecret)
            }

            state = state.copy(
                storeRecoveryTotpSecretResource = submitRecoveryTotpSecretResource
            )
        }
    }

    private fun generateRecoveryTotp(encryptedTotpSecret: Base64EncodedData, instant: Instant = Clock.System.now()): ApproverAccessState.RecoveryTotpState {
        val counter = instant.epochSeconds.div(TotpGenerator.CODE_EXPIRATION)

        return ApproverAccessState.RecoveryTotpState(
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
        viewModelScope.launch {
            state = state.copy(recoveryTotp = generateRecoveryTotp(encryptedSecret))
            recoveryTotpTimer.startCountDownTimer(UPDATE_COUNTDOWN) {
                state.recoveryTotp?.also { totp ->
                    val now = Clock.System.now()

                    state =
                        if (totp.counter != now.epochSeconds.div(TotpGenerator.CODE_EXPIRATION)) {
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
    }

    private fun stopRecoveryTotpGeneration() {
        recoveryTotpTimer.stopCountDownTimer()
        state = state.copy(recoveryTotp = null)
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

        when (state.approverAccessUIState) {
            ApproverAccessUIState.AccessRequested,
            ApproverAccessUIState.UserNeedsPasteRecoveryLink,
            ApproverAccessUIState.VerifyingToTPFromOwner,
            ApproverAccessUIState.WaitingForToTPFromOwner -> cancelRecovery()

            ApproverAccessUIState.AccessApproved,
            ApproverAccessUIState.Complete -> {}
        }
    }

    private fun cancelRecovery() {
        guardianRepository.clearParticipantId()

        state = state.copy(
            participantId = "",
            approverAccessUIState = ApproverAccessUIState.Complete
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

    //region CloudStorage Action methods
    fun handleCloudStorageActionSuccess(
        privateKey: Base58EncodedPrivateKey,
        cloudStorageAction: CloudStorageActions
    ) {
        state = state.copy(
            cloudStorageAction = CloudStorageActionData(),
            loadKeyFromCloudResource = Resource.Uninitialized
        )

        when (cloudStorageAction) {
            CloudStorageActions.DOWNLOAD -> {
                keyDownloadSuccess(privateEncryptionKey = privateKey.toEncryptionKey())
            }
            else -> {}
        }
    }

    private fun keyDownloadSuccess(privateEncryptionKey: EncryptionKey) {
        state = state.copy(guardianEncryptionKey = privateEncryptionKey)

        checkRecoveryConfirmationPhase()
    }

    private fun checkRecoveryConfirmationPhase() {
        //Grab the recoveryConfirmation data from state
        val recoveryConfirmation = state.recoveryConfirmationPhase
        if (recoveryConfirmation != null) {
            confirmOrRejectOwnerAccessRequest(ParticipantId(state.participantId), recoveryConfirmation)
        } else {
            //TODO: Log with raygun
            state =
                state.copy(approveRecoveryResource = Resource.Error(exception = Exception("Unable to confirm owner recovery, missing recovery confirmation data")))
        }
    }

    fun handleCloudStorageActionFailure(
        exception: Exception?,
        cloudStorageAction: CloudStorageActions
    ) {
        state = state.copy(
            cloudStorageAction = CloudStorageActionData(),
            loadKeyFromCloudResource = Resource.Uninitialized
        )

        when (cloudStorageAction) {
            CloudStorageActions.DOWNLOAD -> {
                setErrorToApproveRecoveryResource(exception)
            }
            else -> {}
        }
    }

    private fun setErrorToApproveRecoveryResource(exception: Exception?) {
        state = state.copy(approveRecoveryResource = Resource.Error(
            exception = exception
        ))
    }

    fun userPastedRecovery(clipboardContent: String?) {
        val participantId = clipboardContent?.getInviteCodeFromDeeplink()

        if (!participantId.isNullOrEmpty()) {
            guardianRepository.saveParticipantId(participantId)
            assignParticipantId(participantId)
            state = state.copy(
                onboardingMessage = Resource.Success(RecoveryMessage.LinkPastedSuccessfully)
            )
            retrieveApproverState(false)
        } else {
            state = state.copy(
                onboardingMessage = Resource.Success(RecoveryMessage.FailedPasteLink)
            )
        }
    }

    //endregion
}