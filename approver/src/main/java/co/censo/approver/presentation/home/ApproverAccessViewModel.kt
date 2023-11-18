package co.censo.approver.presentation.home

import Base58EncodedPrivateKey
import Base64EncodedData
import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.approver.data.ApproverAccessUIState
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.SymmetricEncryption
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.cryptography.base64Encoded
import co.censo.shared.data.cryptography.decryptWithEntropy
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.cryptography.key.ExternalEncryptionKey
import co.censo.shared.data.cryptography.sha256digest
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
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.VaultCountDownTimer
import co.censo.shared.util.sendError
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
class ApproverAccessViewModel @Inject constructor(
    private val guardianRepository: GuardianRepository,
    private val keyRepository: KeyRepository,
    private val recoveryTotpTimer: VaultCountDownTimer,
    private val userStatePollingTimer: VaultCountDownTimer
) : ViewModel() {

    var state by mutableStateOf(ApproverAccessState())
        private set

    fun onStart() {
        retrieveApproverState(false)

        userStatePollingTimer.start(POLLING_VERIFICATION_COUNTDOWN) {
            if (state.shouldCheckRecoveryCode) {
                retrieveApproverState(true)
            }
        }
    }

    fun onStop() {
        userStatePollingTimer.stop()
        stopRecoveryTotpGeneration()
    }

    fun retrieveApproverState(silently: Boolean) {
        if (!silently) {
            state = state.copy(userResponse = Resource.Loading())
        }

        viewModelScope.launch {
            val userResponse = guardianRepository.retrieveUser()

            state = state.copy(userResponse = userResponse)

            if (userResponse is Resource.Success) {
                val guardianStates = userResponse.data!!.guardianStates
                checkApproverHasParticipantData(guardianStates)
            }
        }
    }

    private fun checkApproverHasParticipantData(guardianStates: List<GuardianState>) {
        val participantId = guardianRepository.retrieveParticipantId()
        state = state.copy(approvalId = guardianRepository.retrieveApprovalId())

        //If participantId is empty go back to the paste link
        if (participantId.isEmpty()) {
            state = state.copy(navToApproverRouting = true)
            return
        }

        when (val guardianState = guardianStates.forParticipant(participantId = participantId)) {
            null -> {
                guardianRepository.clearParticipantId()
                state = state.copy(navToApproverRouting = true)
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


    private fun determineApproverAccessUIState(guardianState: GuardianState) {
        state = when (val phase = guardianState.phase) {
            is GuardianPhase.Complete -> {
                when (state.approveRecoveryResource) {
                    is Resource.Success -> {
                        guardianRepository.clearApprovalId()
                        guardianRepository.clearParticipantId()
                        state.copy(approverAccessUIState = ApproverAccessUIState.Complete)
                    }
                    else -> {
                        state.copy(accessNotInProgress = Resource.Error())
                    }
                }
            }

            is GuardianPhase.RecoveryRequested ->
                state.copy(
                    approverAccessUIState = ApproverAccessUIState.AccessRequested,
                    guardianState = guardianState
                )
            is GuardianPhase.RecoveryVerification -> {
                startRecoveryTotpGeneration(phase.encryptedTotpSecret)
                state.copy(
                    approverAccessUIState = ApproverAccessUIState.WaitingForToTPFromOwner,
                    guardianState = guardianState
                )
            }
            is GuardianPhase.RecoveryConfirmation -> {
                loadPrivateKeyFromCloud(phase)
                state.copy(
                    approverAccessUIState = ApproverAccessUIState.VerifyingToTPFromOwner,
                    guardianState = guardianState
                )
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

                val guardianKey = state.guardianEncryptionKey
                val entropy = recoveryConfirmation.guardianEntropy

                if (guardianKey == null) {
                    state = state.copy(approveRecoveryResource = Resource.Uninitialized)
                    loadPrivateKeyFromCloud(recoveryPhase = recoveryConfirmation)
                    return@launch
                }

                if (entropy == null) {
                    state =
                        state.copy(approveRecoveryResource = Resource.Error(
                            exception = Exception("Unable to approve this access request, missing information"))
                        )
                    return@launch
                }

                val key = guardianKey.key.decryptWithEntropy(
                    deviceKeyId = keyRepository.retrieveSavedDeviceId(),
                    entropy = entropy
                )

                val ownerPublicKey = ExternalEncryptionKey.generateFromPublicKeyBase58(recoveryConfirmation.ownerPublicKey)


                val encryptedShard = ownerPublicKey.encrypt(
                    key.toEncryptionKey().decrypt(recoveryConfirmation.guardianEncryptedShard.bytes)
                ).base64Encoded()

                val response =
                    if (state.approvalId.isNotEmpty()) {
                        guardianRepository.approveAccess(
                            approvalId = state.approvalId,
                            encryptedShard = encryptedShard
                        )
                    } else {
                        guardianRepository.approveRecovery(
                            participantId = participantId,
                            encryptedShard = encryptedShard
                        )
                    }

                state = state.copy(approveRecoveryResource = response, ownerEnteredWrongCode = false)

                if (response is Resource.Success) {
                    guardianRepository.clearApprovalId()
                    guardianRepository.clearParticipantId()
                    determineApproverAccessUIState(response.data?.guardianStates?.forParticipant(state.participantId)!!)
                }

                stopRecoveryTotpGeneration()
            } else {
                state = state.copy(rejectRecoveryResource = Resource.Loading())
                val response =
                    if (state.approvalId.isNotEmpty()) {
                        guardianRepository.rejectAccess(state.approvalId)
                    } else {
                        guardianRepository.rejectRecovery(participantId)
                    }
                state = state.copy(rejectRecoveryResource = response, ownerEnteredWrongCode = true)
                if (response is Resource.Success) {
                    guardianRepository.clearApprovalId()
                    guardianRepository.clearParticipantId()
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

            val submitRecoveryTotpSecretResource =
                if (state.approvalId.isNotEmpty()) {
                    guardianRepository.storeAccessTotpSecret(
                        approvalId = state.approvalId,
                        encryptedTotpSecret = encryptedSecret
                    )
                } else {
                    guardianRepository.storeRecoveryTotpSecret(
                        participantId = state.participantId,
                        encryptedTotpSecret = encryptedSecret
                    )
                }

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
            recoveryTotpTimer.start(UPDATE_COUNTDOWN) {
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
        recoveryTotpTimer.stop()
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

    fun resetApproverRoutingNavigationTrigger() {
        state = state.copy(navToApproverRouting = false)
    }

    fun onTopBarCloseConfirmed() {
        hideCloseConfirmationDialog()

        when (state.approverAccessUIState) {
            ApproverAccessUIState.AccessRequested,
            ApproverAccessUIState.VerifyingToTPFromOwner,
            ApproverAccessUIState.WaitingForToTPFromOwner -> cancelRecovery()

            ApproverAccessUIState.Complete -> {}
        }
    }

    fun triggerApproverRoutingNavigation() {
        state = state.copy(navToApproverRouting = true)
    }


    private fun cancelRecovery() {
        guardianRepository.clearParticipantId()
        guardianRepository.clearApprovalId()

        state = state.copy(
            approvalId = "",
            participantId = "",
            navToApproverRouting = true
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

    fun resetAccessNotInProgressResource() {
        cancelRecovery()
        state = state.copy(accessNotInProgress = Resource.Uninitialized)
    }

    //region CloudStorage Action methods
    fun handleCloudStorageActionSuccess(
        encryptedKey: ByteArray,
        cloudStorageAction: CloudStorageActions
    ) {
        state = state.copy(
            cloudStorageAction = CloudStorageActionData(),
            loadKeyFromCloudResource = Resource.Uninitialized
        )

        when (cloudStorageAction) {
            CloudStorageActions.DOWNLOAD -> {
                keyDownloadSuccess(privateEncryptionKey = encryptedKey)
            }
            else -> {}
        }
    }

    private fun keyDownloadSuccess(privateEncryptionKey: ByteArray) {
        state = state.copy(guardianEncryptionKey = EncryptedKey(privateEncryptionKey))

        checkRecoveryConfirmationPhase()
    }

    private fun checkRecoveryConfirmationPhase() {
        //Grab the recoveryConfirmation data from state
        val recoveryConfirmation = state.recoveryConfirmationPhase
        if (recoveryConfirmation != null) {
            confirmOrRejectOwnerAccessRequest(ParticipantId(state.participantId), recoveryConfirmation)
        } else {
            Exception().sendError(CrashReportingUtil.RecoveryConfirmation)
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

    //endregion
}