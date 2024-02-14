package co.censo.approver.presentation.home

import Base64EncodedData
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.approver.data.ApproverAccessUIState
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.cryptography.base64Encoded
import co.censo.shared.data.cryptography.decryptWithEntropy
import co.censo.shared.data.cryptography.key.ExternalEncryptionKey
import co.censo.shared.data.model.ApproverPhase
import co.censo.shared.data.model.ApproverState
import co.censo.shared.data.model.forParticipant
import co.censo.shared.data.repository.ApproverRepository
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.storage.CloudStoragePermissionNotGrantedException
import co.censo.shared.util.CountDownTimerImpl
import co.censo.shared.util.CountDownTimerImpl.Companion.POLLING_VERIFICATION_COUNTDOWN
import co.censo.shared.util.CountDownTimerImpl.Companion.UPDATE_COUNTDOWN
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.VaultCountDownTimer
import co.censo.shared.util.observeCloudAccessStateForAccessGranted
import co.censo.shared.util.sendError
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
    private val approverRepository: ApproverRepository,
    private val keyRepository: KeyRepository,
    private val accessTotpTimer: VaultCountDownTimer,
    private val userStatePollingTimer: VaultCountDownTimer,
    private val totpGenerator: TotpGenerator
) : ViewModel() {

    var state by mutableStateOf(ApproverAccessState())
        private set

    fun onStart() {
        retrieveApproverState(false)

        userStatePollingTimer.start(POLLING_VERIFICATION_COUNTDOWN) {
            if (state.shouldCheckAccessCode) {
                retrieveApproverState(true)
            }
        }
    }

    fun onStop() {
        userStatePollingTimer.stopWithDelay(CountDownTimerImpl.Companion.VERIFICATION_STOP_DELAY)
        stopAccessTotpGeneration()
    }

    fun retrieveApproverState(silently: Boolean) {
        if (!silently) {
            state = state.copy(userResponse = Resource.Loading)
        }

        viewModelScope.launch {
            val userResponse = approverRepository.retrieveUser()

            state = state.copy(userResponse = userResponse)

            if (userResponse is Resource.Success) {
                val approverStates = userResponse.data.approverStates
                checkApproverHasParticipantData(approverStates)
            }
        }
    }

    private fun checkApproverHasParticipantData(approverStates: List<ApproverState>) {
        val participantId = approverRepository.retrieveParticipantId()
        state = state.copy(approvalId = approverRepository.retrieveApprovalId())

        //If participantId is empty go back to the paste link
        if (participantId.isEmpty()) {
            state = state.copy(navToApproverEntrance = true)
            return
        }

        when (val approverState = approverStates.forParticipant(participantId = participantId)) {
            null -> {
                approverRepository.clearParticipantId()
                state = state.copy(navToApproverEntrance = true)
            }
            else -> {
                assignApproverStateAndParticipantId(
                    approverState = approverState,
                    participantId = participantId
                )
                determineApproverAccessUIState(approverState)
            }
        }
    }

    private fun assignApproverStateAndParticipantId(approverState: ApproverState?, participantId: String) {
        state = state.copy(participantId = participantId, approverState = approverState)
    }


    private fun determineApproverAccessUIState(approverState: ApproverState) {
        state = when (val phase = approverState.phase) {
            is ApproverPhase.Complete -> {
                when (state.approveAccessResource) {
                    is Resource.Success -> {
                        approverRepository.clearApprovalId()
                        approverRepository.clearParticipantId()
                        state.copy(approverAccessUIState = ApproverAccessUIState.Complete)
                    }
                    else -> {
                        state.copy(accessNotInProgress = Resource.Error())
                    }
                }
            }

            is ApproverPhase.AccessRequested ->
                state.copy(
                    approverAccessUIState = ApproverAccessUIState.AccessRequested,
                    approverState = approverState
                )
            is ApproverPhase.AccessVerification -> {
                startAccessTotpGeneration(phase.encryptedTotpSecret)
                state.copy(
                    approverAccessUIState = ApproverAccessUIState.WaitingForToTPFromOwner,
                    approverState = approverState
                )
            }
            is ApproverPhase.AccessConfirmation -> {
                loadPrivateKeyFromCloud(phase)
                state.copy(
                    approverAccessUIState = ApproverAccessUIState.VerifyingToTPFromOwner,
                    approverState = approverState
                )
            }

            else -> state
        }
    }

    private fun confirmOrRejectOwnerAccessRequest(accessConfirmation: ApproverPhase.AccessConfirmation) {
        val accessTotp = generateAccessTotp(
            accessConfirmation.encryptedTotpSecret,
            Instant.fromEpochMilliseconds(accessConfirmation.ownerKeySignatureTimeMillis)
        )

        val totpIsCorrect = approverRepository.checkTotpMatches(
            accessTotp.encryptedSecret,
            accessConfirmation.ownerPublicKey,
            signature = accessConfirmation.ownerKeySignature,
            timeMillis = accessConfirmation.ownerKeySignatureTimeMillis
        )

        viewModelScope.launch(Dispatchers.IO) {
            if (totpIsCorrect) {
                state = state.copy(approveAccessResource = Resource.Loading)

                val approverKey = state.approverEncryptionKey
                val entropy = accessConfirmation.approverEntropy

                if (approverKey == null) {
                    state = state.copy(approveAccessResource = Resource.Uninitialized)
                    loadPrivateKeyFromCloud(accessPhase = accessConfirmation)
                    return@launch
                }

                val key = approverKey.key.decryptWithEntropy(
                    deviceKeyId = keyRepository.retrieveSavedDeviceId(),
                    entropy = entropy
                )

                val ownerPublicKey = ExternalEncryptionKey.generateFromPublicKeyBase58(accessConfirmation.ownerPublicKey)


                val encryptedShard = ownerPublicKey.encrypt(
                    key.toEncryptionKey().decrypt(accessConfirmation.approverEncryptedShard.bytes)
                ).base64Encoded()

                val response = approverRepository.approveAccess(
                    approvalId = state.approvalId,
                    encryptedShard = encryptedShard
                )

                state = state.copy(approveAccessResource = response, ownerEnteredWrongCode = false)

                if (response is Resource.Success) {
                    approverRepository.clearApprovalId()
                    approverRepository.clearParticipantId()
                    determineApproverAccessUIState(response.data.approverStates.forParticipant(state.participantId)!!)
                }

                stopAccessTotpGeneration()
                userStatePollingTimer.stop()
            } else {
                state = state.copy(rejectAccessResource = Resource.Loading)
                val response = approverRepository.rejectAccess(state.approvalId)
                state = state.copy(rejectAccessResource = response, ownerEnteredWrongCode = true)
                if (response is Resource.Success) {
                    determineApproverAccessUIState(response.data.approverStates.forParticipant(state.participantId)!!)
                }
            }
        }
    }

    fun storeAccessTotpSecret() {
        state = state.copy(storeAccessTotpSecretResource = Resource.Loading)

        viewModelScope.launch {
            val secret = totpGenerator.generateSecret()
            val encryptedSecret = keyRepository
                .encryptWithDeviceKey(secret.toByteArray())
                .base64Encoded()

            val submitAccessTotpSecretResource = approverRepository.storeAccessTotpSecret(
                approvalId = state.approvalId,
                encryptedTotpSecret = encryptedSecret
            )

            if (submitAccessTotpSecretResource is Resource.Success) {
                determineApproverAccessUIState(
                    submitAccessTotpSecretResource.data.approverStates.forParticipant(
                        state.participantId
                    )!!
                )
                startAccessTotpGeneration(encryptedSecret)
            }

            state = state.copy(
                storeAccessTotpSecretResource = submitAccessTotpSecretResource
            )
        }
    }

    private fun generateAccessTotp(encryptedTotpSecret: Base64EncodedData, instant: Instant = Clock.System.now()): ApproverAccessState.AccessTotpState {
        val counter = instant.epochSeconds.div(TotpGenerator.CODE_EXPIRATION)

        return ApproverAccessState.AccessTotpState(
            code = totpGenerator.generateCode(
                secret = String(keyRepository.decryptWithDeviceKey(encryptedTotpSecret.bytes)),
                counter = counter,
            ),
            counter = counter,
            currentSecond = instant.toLocalDateTime(TimeZone.UTC).second,
            encryptedSecret = encryptedTotpSecret
        )
    }

    private fun startAccessTotpGeneration(encryptedSecret: Base64EncodedData) {
        viewModelScope.launch {
            state = state.copy(accessTotp = generateAccessTotp(encryptedSecret))
            accessTotpTimer.start(UPDATE_COUNTDOWN) {
                state.accessTotp?.also { totp ->
                    val now = Clock.System.now()

                    state =
                        if (totp.counter != now.epochSeconds.div(TotpGenerator.CODE_EXPIRATION)) {
                            state.copy(
                                accessTotp = generateAccessTotp(totp.encryptedSecret)
                            )
                        } else {
                            state.copy(
                                accessTotp = totp.copy(
                                    currentSecond = now.toLocalDateTime(TimeZone.UTC).second
                                )
                            )
                        }
                }
            }
        }
    }

    private fun stopAccessTotpGeneration() {
        accessTotpTimer.stop()
        state = state.copy(accessTotp = null)
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

    fun resetApproverEntranceNavigationTrigger() {
        state = state.copy(navToApproverEntrance = false)
    }

    fun onTopBarCloseConfirmed() {
        hideCloseConfirmationDialog()
        cancelAccess()
    }

    private fun cancelAccess() {
        approverRepository.clearParticipantId()
        approverRepository.clearApprovalId()

        state = state.copy(
            approvalId = "",
            participantId = "",
            navToApproverEntrance = true
        )
    }

    fun resetStoreAccessTotpSecretResource() {
        state = state.copy(
            storeAccessTotpSecretResource = Resource.Uninitialized
        )
    }

    fun resetApproveAccessResource() {
        state = state.copy(approveAccessResource = Resource.Uninitialized)
    }

    fun resetRejectAccessResource() {
        state = state.copy(rejectAccessResource = Resource.Uninitialized)
    }

    fun resetAccessNotInProgressResource() {
        cancelAccess()
        state = state.copy(accessNotInProgress = Resource.Uninitialized)
    }

    //region CloudStorage Action methods
    private fun loadPrivateKeyFromCloud(
        accessPhase: ApproverPhase.AccessConfirmation,
        bypassScopeCheck: Boolean = false
    ) {
        state = state.copy(
            accessConfirmationPhase = accessPhase,
            loadKeyFromCloudResource = Resource.Loading
        )

        viewModelScope.launch(Dispatchers.IO) {
            val downloadResponse = try {
                keyRepository.retrieveKeyFromCloud(
                    id = state.participantId,
                    bypassScopeCheck = bypassScopeCheck,
                )
            } catch (permissionNotGranted: CloudStoragePermissionNotGrantedException) {
                observeCloudAccessStateForAccessGranted(
                    coroutineScope = this, keyRepository = keyRepository
                ) {
                    //Retry this method
                    loadPrivateKeyFromCloud(accessPhase = accessPhase, bypassScopeCheck = true)
                }
                return@launch
            }

            state = state.copy(loadKeyFromCloudResource = Resource.Uninitialized)

            if (downloadResponse is Resource.Success) {
                keyDownloadSuccess(privateEncryptionKey = downloadResponse.data)
            } else if (downloadResponse is Resource.Error) {
                setErrorToApproveAccessResource(downloadResponse.exception)
            }
        }
    }

    private fun keyDownloadSuccess(privateEncryptionKey: ByteArray) {
        state = state.copy(approverEncryptionKey = EncryptedKey(privateEncryptionKey))

        checkAccessConfirmationPhase()
    }

    private fun checkAccessConfirmationPhase() {
        //Grab the accessConfirmation data from state
        val accessConfirmation = state.accessConfirmationPhase
        if (accessConfirmation != null) {
            confirmOrRejectOwnerAccessRequest(accessConfirmation)
        } else {
            Exception().sendError(CrashReportingUtil.AccessConfirmation)
            state =
                state.copy(approveAccessResource = Resource.Error(exception = Exception("Unable to confirm owner access, missing access confirmation data")))
        }
    }

    private fun setErrorToApproveAccessResource(exception: Exception?) {
        state = state.copy(approveAccessResource = Resource.Error(
            exception = exception
        ))
    }

    //endregion
}