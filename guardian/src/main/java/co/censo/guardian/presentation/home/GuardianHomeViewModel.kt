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
import co.censo.guardian.routingLogTag
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
import co.censo.shared.presentation.cloud_storage.CloudStorageActionData
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.util.CountDownTimerImpl.Companion.POLLING_VERIFICATION_COUNTDOWN
import co.censo.shared.util.CountDownTimerImpl.Companion.UPDATE_COUNTDOWN
import co.censo.shared.util.VaultCountDownTimer
import co.censo.shared.util.projectLog
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.novacrypto.base58.Base58
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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

    val guardianHomeLogTag = "$routingLogTag + GuardianHome"

    fun onStart() {
        projectLog(tag = guardianHomeLogTag, message = "GuardianHomeVM onStart running")

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
                    projectLog(tag = guardianHomeLogTag, message = "participantId was empty, determining guardianUIState with: ${guardianStates.firstOrNull()}")
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
        viewModelScope.launch(Dispatchers.IO) {
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
                        loadPrivateKeyFromCloud(
                            reason = GuardianHomeCloudStorageReasons.CONFIRM_OR_REJECT_OWNER,
                            recoveryPhase = phase
                        )
                        GuardianUIState.ACCESS_VERIFYING_TOTP_FROM_OWNER
                    }
                }
            }

            val stateParticipantId = guardianState?.participantId?.value ?: state.participantId

            projectLog(tag = guardianHomeLogTag, message = "Approver invite code: $inviteCode")
            projectLog(tag = guardianHomeLogTag, message = "Approver local participantID: ${participantId.ifEmpty { "no local value" }} (Retrieved from local storage)")
            projectLog(tag = guardianHomeLogTag, message = "Approver remote participantID: ${stateParticipantId.ifEmpty { "no remote value" }} (From guardianState/remote)")
            projectLog(tag = guardianHomeLogTag, message = "determined UI State: $guardianUIStateNewNew")

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
                acceptResource.data?.guardianState?.participantId?.let {
                    state = state.copy(participantId = it.value)
                }
                createAndSaveGuardianKey()
            }

            state = state.copy(acceptGuardianResource = acceptResource)
        }
    }

    fun createAndSaveGuardianKey() {
        val guardianEncryptionKey = keyRepository.createGuardianKey()
        state = state.copy(
            savePrivateKeyToCloudResource = Resource.Loading(),
            cloudStorageAction = CloudStorageActionData(
                triggerAction = true, action = CloudStorageActions.UPLOAD, reason = null
            ),
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
                loadPrivateKeyFromCloud(
                    GuardianHomeCloudStorageReasons.SUBMIT_VERIFICATION_CODE
                )
            }
        }
    }

    fun submitVerificationCode() {
        state = state.copy(submitVerificationResource = Resource.Loading())

        viewModelScope.launch(Dispatchers.IO) {

            if (state.guardianEncryptionKey == null) {
                loadPrivateKeyFromCloud(reason = GuardianHomeCloudStorageReasons.SUBMIT_VERIFICATION_CODE)
                return@launch
            }

            if (state.invitationId.value.isEmpty()) {
                loadInvitationId()
            }


            val signedVerificationData  = try {
                guardianRepository.signVerificationCode(
                    verificationCode = state.verificationCode,
                    state.guardianEncryptionKey!!
                )
            } catch (e: Exception) {
                state = state.copy(
                    submitVerificationResource = Resource.Error(exception = e),
                    verificationCode = ""
                )
                return@launch
            }

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

    private fun loadPrivateKeyFromCloud(
        reason: GuardianHomeCloudStorageReasons,
        recoveryPhase: GuardianPhase.RecoveryConfirmation? = null
    ) {
        state = state.copy(
            cloudStorageAction = CloudStorageActionData(
                triggerAction = true, action = CloudStorageActions.DOWNLOAD, reason = reason
            ),
            recoveryConfirmationPhase = recoveryPhase
        )
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

    private fun confirmOrRejectOwnerRecovery(
        participantId: ParticipantId, recoveryConfirmation: GuardianPhase.RecoveryConfirmation
    ) {
        val recoveryTotp = generateRecoveryTotp(
            recoveryConfirmation.encryptedTotpSecret,
            Instant.fromEpochMilliseconds(recoveryConfirmation.ownerKeySignatureTimeMillis)
        )

        val totpIsCorrect = guardianRepository.checkTotpMatches(
            recoveryTotp.code,
            recoveryConfirmation.ownerPublicKey,
            signature = recoveryConfirmation.ownerKeySignature,
            timeMillis = recoveryConfirmation.ownerKeySignatureTimeMillis
        )

        viewModelScope.launch(Dispatchers.IO) {
            if (totpIsCorrect) {
                if (state.guardianEncryptionKey == null) {

                    //Set to state so we can retry this method with the same parameters after the key is loaded
                    loadPrivateKeyFromCloud(
                        reason = GuardianHomeCloudStorageReasons.CONFIRM_OR_REJECT_OWNER,
                        recoveryPhase = recoveryConfirmation
                    )
                    return@launch
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

    //region CloudStorage Action methods
    fun getPrivateKeyForUpload() : Base58EncodedPrivateKey? {
        val encryptionKey = state.guardianEncryptionKey ?: return null
        return Base58EncodedPrivateKey(Base58.base58Encode(encryptionKey.privateKeyRaw()))
    }

    fun getDownloadReason(): GuardianHomeCloudStorageReasons =
        try {
            state.cloudStorageAction.reason as GuardianHomeCloudStorageReasons
        } catch (e: Exception) {
            GuardianHomeCloudStorageReasons.NONE
        }

    fun handleCloudStorageActionSuccess(
        privateKey: Base58EncodedPrivateKey,
        cloudStorageAction: CloudStorageActions
    ) {
        val downloadReason = getDownloadReason()
        state = state.copy(cloudStorageAction = CloudStorageActionData())

        when (cloudStorageAction) {
            CloudStorageActions.UPLOAD -> {
                keyUploadSuccess(privateKey.toEncryptionKey())
            }
            CloudStorageActions.DOWNLOAD -> {
                keyDownloadSuccess(
                    privateEncryptionKey = privateKey.toEncryptionKey(),
                    downloadReason = downloadReason
                )
            }
            else -> {}
        }
    }

    //region handle key success
    private fun keyUploadSuccess(privateEncryptionKey: EncryptionKey) {
        //User uploaded key successfully, move forward by retrieving user state
        state = state.copy(
            guardianEncryptionKey = privateEncryptionKey,
            savePrivateKeyToCloudResource = Resource.Uninitialized
        )
        retrieveApproverState()
    }

    private fun keyDownloadSuccess(
        privateEncryptionKey: EncryptionKey,
        downloadReason: GuardianHomeCloudStorageReasons
    ) {
        state = state.copy(guardianEncryptionKey = privateEncryptionKey)

        when (downloadReason) {
            GuardianHomeCloudStorageReasons.CONFIRM_OR_REJECT_OWNER -> {
                confirmRejectOwner()
            }
            GuardianHomeCloudStorageReasons.SUBMIT_VERIFICATION_CODE -> {
                submitVerificationCode()
            }
            else -> {}
        }
    }

    private fun confirmRejectOwner() {
        //Grab the recoveryConfirmation data from state
        val recoveryConfirmation = state.recoveryConfirmationPhase
        if (recoveryConfirmation != null) {
            confirmOrRejectOwnerRecovery(ParticipantId(state.participantId), recoveryConfirmation)
        } else {
            //TODO: Log with raygun
            projectLog(message = "Recovery confirmation was null, unable to continue confirm/reject owner flow")
            state =
                state.copy(approveRecoveryResource = Resource.Error(exception = Exception("Unable to confirm owner recovery, missing recovery confirmation data")))
        }
    }
    //endregion

    fun handleCloudStorageActionFailure(
        exception: Exception?,
        cloudStorageAction: CloudStorageActions
    ) {

        val downloadReason = getDownloadReason()
        state = state.copy(cloudStorageAction = CloudStorageActionData())

        when (cloudStorageAction) {
            CloudStorageActions.UPLOAD -> {
                keyUploadFailure(exception)
            }
            CloudStorageActions.DOWNLOAD -> {
                keyDownloadFailure(exception, downloadReason)
            }
            else -> {}
        }
    }

    //region handle key failure
    private fun keyUploadFailure(exception: Exception?) {
        state = state.copy(savePrivateKeyToCloudResource = Resource.Error(exception = exception))
    }

    private fun keyDownloadFailure(exception: Exception?, downloadReason: GuardianHomeCloudStorageReasons) {
        when (downloadReason) {
            GuardianHomeCloudStorageReasons.CONFIRM_OR_REJECT_OWNER -> {
                setErrorToApproveRecoveryResource(exception)
            }
            GuardianHomeCloudStorageReasons.SUBMIT_VERIFICATION_CODE -> {
                setErrorToSubmitVerificationResource(exception)
            }
            else -> {}
        }
    }

    private fun setErrorToApproveRecoveryResource(exception: Exception?) {
        //Set error state for the approveRecovery resource
        state = state.copy(approveRecoveryResource = Resource.Error(
            exception = exception
        ))
    }

    private fun setErrorToSubmitVerificationResource(exception: Exception?) {
        //Set error state for the submitVerificationCode resource
        state = state.copy(submitVerificationResource = Resource.Error(
            exception = exception
        ))
    }
    //endregion

    //endregion
}