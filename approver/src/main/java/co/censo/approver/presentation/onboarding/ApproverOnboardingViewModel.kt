package co.censo.approver.presentation.onboarding

import Base58EncodedPrivateKey
import InvitationId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.approver.data.ApproverOnboardingUIState
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.SymmetricEncryption
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.cryptography.decryptFromByteArray
import co.censo.shared.data.cryptography.encryptToByteArray
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.cryptography.sha256digest
import co.censo.shared.data.cryptography.toByteArrayNoSign
import co.censo.shared.data.model.GuardianPhase
import co.censo.shared.data.model.GuardianState
import co.censo.shared.data.repository.GuardianRepository
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.presentation.cloud_storage.CloudStorageActionData
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.util.CountDownTimerImpl
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.VaultCountDownTimer
import co.censo.shared.util.sendError
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.novacrypto.base58.Base58
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApproverOnboardingViewModel @Inject constructor(
    private val guardianRepository: GuardianRepository,
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository,
    private val userStatePollingTimer: VaultCountDownTimer
) : ViewModel() {

    var state by mutableStateOf(ApproverOnboardingState())
        private set

    fun onStart() {
        retrieveApproverState(silently = false)

        userStatePollingTimer.start(CountDownTimerImpl.Companion.POLLING_VERIFICATION_COUNTDOWN) {
            if (state.userResponse !is Resource.Loading
                && state.savePrivateKeyToCloudResource !is Resource.Loading
                && state.guardianState?.phase is GuardianPhase.WaitingForVerification) {
                retrieveApproverState(silently = true, checkingVerification = true)
            }
        }
    }

    fun onStop() {
        userStatePollingTimer.stop()
    }

    fun onDispose() {
        state = ApproverOnboardingState()
    }

    fun retrieveApproverState(silently: Boolean, checkingVerification: Boolean = false) {
        if (!silently) {
            state = state.copy(userResponse = Resource.Loading())
        }

        viewModelScope.launch {
            val userResponse = ownerRepository.retrieveUser()

            state = state.copy(userResponse = userResponse)

            if (userResponse is Resource.Success) {
                val guardianState = userResponse.data!!.guardianStates.firstOrNull()
                checkApproverHasInvitationCode(userResponse.data!!.guardianStates)

                showMessageWhenUserMovesToComplete(guardianState, checkingVerification)
            }
        }
    }

    private fun checkApproverHasInvitationCode(guardianStates: List<GuardianState>) {
        loadInvitationId()
        val guardianState = guardianStates.firstOrNull { it.invitationId == state.invitationId  }

        determineApproverUIState(guardianState)
    }

    private fun assignGuardianState(guardianState: GuardianState?) {
        state = state.copy(guardianState = guardianState)
    }
    private fun assignParticipantId(guardianState: GuardianState?) {
        guardianState?.participantId?.let { participantId ->
            state = state.copy(participantId = participantId.value)
        }
    }

    private fun determineApproverUIState(guardianState: GuardianState?) {
        assignGuardianState(guardianState = guardianState)
        assignParticipantId(guardianState = guardianState)

        val guardianPhase = guardianState?.phase

        val guardianNotInOnboarding =
            guardianPhase?.isAccessPhase() == true || guardianPhase is GuardianPhase.Complete

        if (guardianNotInOnboarding) {
            guardianRepository.clearInvitationId()
            state = state.copy(navToApproverRouting = true)
            return
        }

        //Is null always when a user needs to accept
        if (guardianState?.phase == null) {
            acceptGuardianship()
            return
        }

        state = when (guardianState.phase) {
            is GuardianPhase.VerificationRejected ->
                state.copy(approverUIState = ApproverOnboardingUIState.CodeRejected)

            is GuardianPhase.WaitingForCode ->
                state.copy(approverUIState = ApproverOnboardingUIState.NeedsToEnterCode)

            is GuardianPhase.WaitingForVerification ->
                state.copy(approverUIState = ApproverOnboardingUIState.WaitingForConfirmation)

            else -> state
        }
    }

    private fun acceptGuardianship() {
        state = state.copy(acceptGuardianResource = Resource.Loading())

        viewModelScope.launch {
            val acceptResource = guardianRepository.acceptGuardianship(
                invitationId = state.invitationId,
            )

            state = state.copy(acceptGuardianResource = acceptResource)

            if (acceptResource is Resource.Success) {
                state = state.copy(
                    onboardingMessage = Resource.Success(OnboardingMessage.LinkAccepted)
                )
                assignParticipantId(acceptResource.data?.guardianState)
                createKeyAndTriggerCloudSave()
            }
        }
    }

    fun createKeyAndTriggerCloudSave() {
        val guardianEncryptionKey = keyRepository.createGuardianKey()
        state = state.copy(
            savePrivateKeyToCloudResource = Resource.Loading(),
            cloudStorageAction = CloudStorageActionData(
                triggerAction = true,
                action = CloudStorageActions.UPLOAD,
                reason = null
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
                checkForPrivateKeyBeforeSubmittingVerificationCode()
            }
        }
    }

    private fun checkForPrivateKeyBeforeSubmittingVerificationCode() {
        state.guardianEncryptionKey?.let {
            submitVerificationCode()
        } ?: loadPrivateKeyFromCloud()
    }

    fun submitVerificationCode() {
        state = state.copy(submitVerificationResource = Resource.Loading())

        viewModelScope.launch(Dispatchers.IO) {

            if (state.guardianEncryptionKey == null) {
                loadPrivateKeyFromCloud()
                return@launch
            }

            if (state.invitationId.value.isEmpty()) {
                loadInvitationId()
            }

            val signedVerificationData  = try {
                guardianRepository.signVerificationCode(
                    verificationCode = state.verificationCode,
                    encryptionKey = state.guardianEncryptionKey!!
                )
            } catch (e: Exception) {
                e.sendError(CrashReportingUtil.SubmitVerification)
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
                determineApproverUIState(submitVerificationResource.data?.guardianState)
                state.copy(
                    submitVerificationResource = submitVerificationResource,
                    verificationCode = ""
                )
            } else {
                state.copy(submitVerificationResource = submitVerificationResource)
            }
        }
    }

    private fun loadPrivateKeyFromCloud() {
        state = state.copy(
            retrievePrivateKeyFromCloudResource = Resource.Loading(),
            cloudStorageAction = CloudStorageActionData(
                triggerAction = true,
                action = CloudStorageActions.DOWNLOAD,
            ),
        )
    }

    private fun loadInvitationId() {
        state = state.copy(
            invitationId = InvitationId(guardianRepository.retrieveInvitationId())
        )
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

        when (state.approverUIState) {
            // onboarding
            ApproverOnboardingUIState.NeedsToEnterCode,
            ApproverOnboardingUIState.WaitingForConfirmation,
            ApproverOnboardingUIState.CodeRejected -> {
                cancelOnboarding()
            }

            //No UI for these states
            ApproverOnboardingUIState.Complete -> {
            }
        }
    }

    private fun cancelOnboarding() {
        guardianRepository.clearInvitationId()

        state = state.copy(
            invitationId = InvitationId(""),
            navToApproverRouting = true
        )
    }

    fun resetAcceptGuardianResource() {
        state = state.copy(
            acceptGuardianResource = Resource.Uninitialized
        )
        cancelOnboarding()
    }

    fun resetSubmitVerificationResource() {
        state = state.copy(
            submitVerificationResource = Resource.Uninitialized
        )
    }

    fun resetApproverRoutingNavigationTrigger() {
        state = state.copy(navToApproverRouting = false)
    }

    //region CloudStorage Action methods
    fun getEncryptedKeyForUpload() : ByteArray? {
        val encryptionKey = state.guardianEncryptionKey ?: return null
        return encryptionKey.encryptToByteArray(keyRepository.retrieveSavedDeviceId())
    }

    fun handleCloudStorageActionSuccess(
        encryptedKey: ByteArray,
        cloudStorageAction: CloudStorageActions
    ) {
        state = state.copy(cloudStorageAction = CloudStorageActionData())

        //Decrypt the byteArray
        val privateKey =
            encryptedKey.decryptFromByteArray(keyRepository.retrieveSavedDeviceId())

        when (cloudStorageAction) {
            CloudStorageActions.UPLOAD -> {
                keyUploadSuccess(privateKey.toEncryptionKey())
            }
            CloudStorageActions.DOWNLOAD -> {
                keyDownloadSuccess(
                    privateEncryptionKey = privateKey.toEncryptionKey(),
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
        retrieveApproverState(false)
    }

    private fun keyDownloadSuccess(
        privateEncryptionKey: EncryptionKey,
    ) {
        state = state.copy(
            retrievePrivateKeyFromCloudResource = Resource.Uninitialized,
            guardianEncryptionKey = privateEncryptionKey
        )
        submitVerificationCode()
    }
    //endregion


    //region handle key failure
    fun handleCloudStorageActionFailure(
        exception: Exception?,
        cloudStorageAction: CloudStorageActions
    ) {

        state = state.copy(cloudStorageAction = CloudStorageActionData())

        when (cloudStorageAction) {
            CloudStorageActions.UPLOAD -> {
                keyUploadFailure(exception)
            }
            CloudStorageActions.DOWNLOAD -> {
                keyDownloadFailure(exception)
            }
            else -> {}
        }
    }

    private fun keyUploadFailure(exception: Exception?) {
        state = state.copy(savePrivateKeyToCloudResource = Resource.Error(exception = exception))
    }

    private fun keyDownloadFailure(exception: Exception?) {
        //Set error state for the submitVerificationCode resource
        state = state.copy(
            retrievePrivateKeyFromCloudResource = Resource.Uninitialized,
            submitVerificationResource = Resource.Error(
                exception = exception
            )
        )
    }

    private fun showMessageWhenUserMovesToComplete(
        guardianState: GuardianState?, checkingVerification: Boolean
    ) {
        if (guardianState?.phase is GuardianPhase.Complete && checkingVerification) {
            guardianRepository.clearInvitationId()
            state = state.copy(
                onboardingMessage = Resource.Success(OnboardingMessage.CodeAccepted)
            )
        }
    }

    fun resetMessage() {
        state = state.copy(onboardingMessage = Resource.Uninitialized)
    }
    //endregion

    //endregion
}