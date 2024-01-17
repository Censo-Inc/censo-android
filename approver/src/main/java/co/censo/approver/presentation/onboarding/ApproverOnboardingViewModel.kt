package co.censo.approver.presentation.onboarding

import InvitationId
import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.approver.presentation.Screen
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.cryptography.decryptWithEntropy
import co.censo.shared.data.cryptography.encryptWithEntropy
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.model.ApproverPhase
import co.censo.shared.data.model.ApproverState
import co.censo.shared.data.repository.ApproverRepository
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.presentation.cloud_storage.CloudStorageActionData
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.util.CountDownTimerImpl
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.VaultCountDownTimer
import co.censo.shared.util.asResource
import co.censo.shared.util.isDigitsOnly
import co.censo.shared.util.sendError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject

/**
 * ApproverOnboardingViewModel
 *
 * High Level Flows
 *
 * - Approvers First Entrance into Onboarding
 *
 *      - On the very first entrance into the onboardingVM, the approvers approverState data will be
 *        retrieved and used to determine the UI states. The approvers approverState.phase is expected
 *        to be null. If the phase is null then the approver has yet to accept approvership and we
 *        will automatically trigger the call to accept approvership. No UI interaction is required at this point.
 *
 *        Once the approvership has been accepted, we will assign a participantId to state, we get
 *        the ID from the accept approvership response. This ID is used to mark the cloud saved key so
 *        we can retrieve it in the future. If the key saving failed, we will put the approver into
 *        a NeedsToSaveKey UI state where they can retry the key creation and saving to the cloud.
 *
 *
 * - Submit verification code
 *
 *      - Once the approver has successfully saved their key to the cloud, we will retrieve their
 *        latest state from the backend to determine the next step. At this point they will be in the
 *        WaitingForCode phase / NeedsToEnterCode UI state. After the approver has entered all 6 digits
 *        for the verification code, we check to see if they have their approver key in state. This
 *        key is necessary to sign the verification code. If the key is not in state, we will trigger a
 *        download action from cloud storage. Once we have the key loaded in state (or if we already
 *        had the key in state) we will submit their signed verification code to the backend.
 *
 *        When we call submitVerificationCode, we will set UI state for WaitingForConfirmation.
 *        This will set the CodeEntry view to loading so the approver knows the code is being processed.
 *        After the response has been received from submitVerificationCode, we will determine the UI
 *        state from the latest backend values. At this point the approver state should be
 *        WaitingForVerification phase / WaitingForConfirmation UI state, while the owner user
 *        verifies the code.
 *
 *        If the verification code was rejected, we will get approver phase VerificationRejected and
 *        set CodeRejected to UI state. At this point the approver can re-enter the code until it is
 *        verified.
 *
 *
 * - Verification code verified
 *
 *      - After the verification code has been successfully verified, we will retrieve the latest
 *        approver state from the backend. At this point the returned approver phase should be
 *        Complete. We will set UI state for Complete, and the approver can leave the app after
 *        having finished their onboarding.
 *
 *
 * - Resuming onboarding
 *
 *      - If at any point the approver left the app during onboarding and needs to pick up where they
 *        left off, the backend will return data that lets us determine the approvers current state
 *        from their approver phase. If an API or CloudStorage call failed, then we will set that
 *        error to state and let the user retry. The only exception to this is when the
 *        acceptApprovership call fails.
 *
 *        If acceptApprovership fails, then we will cancel the onboarding and require the user to
 *        start the process over again via entering in their invite link
 *
 */

@HiltViewModel
class ApproverOnboardingViewModel @Inject constructor(
    private val approverRepository: ApproverRepository,
    private val keyRepository: KeyRepository,
    private val userStatePollingTimer: VaultCountDownTimer
) : ViewModel() {

    var state by mutableStateOf(ApproverOnboardingState())
        private set

    //region Lifecycle Methods
    fun onStart() {
        retrieveApproverState(silently = state.approverUIState !is ApproverOnboardingUIState.Loading)

        userStatePollingTimer.start(CountDownTimerImpl.Companion.POLLING_VERIFICATION_COUNTDOWN) {
            if (state.userResponse !is Resource.Loading
                && state.savePrivateKeyToCloudResource !is Resource.Loading
                && (shouldPollUserState(state.approverState?.phase))
            ) {
                retrieveApproverState(silently = true)
            }
        }
    }

    fun onStop() {
        userStatePollingTimer.stopWithDelay(CountDownTimerImpl.Companion.VERIFICATION_STOP_DELAY)
    }

    fun onDispose() {
        state = ApproverOnboardingState()
    }
    //endregion

    //region User Actions
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
            ApproverOnboardingUIState.NeedsToSaveKey,
            ApproverOnboardingUIState.NeedsToEnterCode,
            ApproverOnboardingUIState.WaitingForConfirmation,
            ApproverOnboardingUIState.CodeRejected -> {
                cancelOnboarding()
            }

            //No UI for these states
            ApproverOnboardingUIState.Loading,
            ApproverOnboardingUIState.Complete -> {
            }
        }
    }

    //endregion

    //region Internal Methods
    private fun setLoadingUIState() {
        state = state.copy(approverUIState = ApproverOnboardingUIState.Loading)
    }

    fun retrieveApproverState(silently: Boolean) {
        if (!silently) {
            setLoadingUIState()
        }

        viewModelScope.launch {
            val userResponse = approverRepository.retrieveUser()

            state = state.copy(userResponse = userResponse)

            if (userResponse is Resource.Success) {
                checkApproverHasInvitationCode(userResponse.data.approverStates)
            }
        }
    }

    private fun checkApproverHasInvitationCode(approverStates: List<ApproverState>) {
        loadInvitationId()
        val approverState = approverStates.firstOrNull { it.invitationId == state.invitationId }

        determineApproverUIState(approverState)
    }

    private fun assignApproverState(approverState: ApproverState?) {
        state = state.copy(approverState = approverState)
    }

    private fun assignParticipantId(approverState: ApproverState?) {
        approverState?.participantId?.let { participantId ->
            state = state.copy(participantId = participantId.value)
        }
    }

    private fun assignEntropy(approverState: ApproverState?) {
        val entropy = when (val phase = approverState?.phase) {
            is ApproverPhase.WaitingForCode -> phase.entropy
            is ApproverPhase.VerificationRejected -> phase.entropy
            else -> null
        }

        entropy?.let {
            state = state.copy(entropy = it)
        }
    }

    private fun determineApproverUIState(approverState: ApproverState?) {
        assignApproverState(approverState = approverState)
        assignParticipantId(approverState = approverState)
        assignEntropy(approverState = approverState)

        val approverPhase = approverState?.phase

        val approverNotInOnboarding =
            approverPhase?.isAccessPhase() == true

        if (approverNotInOnboarding) {
            approverRepository.clearInvitationId()
            state = state.copy(navToApproverEntrance = true)
            return
        }

        //Is null always when a user needs to accept
        if (approverState?.phase == null) {
            acceptApprovership()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (!keyRepository.userHasKeySavedInCloud(ParticipantId(state.participantId))) {
                state = state.copy(approverUIState = ApproverOnboardingUIState.NeedsToSaveKey)
                return@launch
            }

            state = when (approverState.phase) {
                is ApproverPhase.VerificationRejected ->
                    state.copy(approverUIState = ApproverOnboardingUIState.CodeRejected)

                is ApproverPhase.WaitingForCode ->
                    state.copy(approverUIState = ApproverOnboardingUIState.NeedsToEnterCode)

                is ApproverPhase.WaitingForVerification ->
                    state.copy(approverUIState = ApproverOnboardingUIState.WaitingForConfirmation)

                is ApproverPhase.Complete -> {
                    userStatePollingTimer.stop()
                    // we require to label the owner unless it is the first owner for this approver
                    val userResponse = state.userResponse
                    if (userResponse is Resource.Success &&
                        userResponse.data.approverStates.count { it.participantId.value != state.participantId } > 0 &&
                        state.approverState?.ownerLabel == null
                    ) {
                        state.copy(
                            navigationResource = Screen.ApproverLabelOwnerScreen.navTo(ParticipantId(state.participantId)).asResource()
                        )
                    } else {
                        approverRepository.clearInvitationId()
                        state.copy(approverUIState = ApproverOnboardingUIState.Complete)
                    }
                }

                else -> state
            }
        }
    }

    /**
     * When this method is triggered, we are already in Loading state for ApproverOnboardingUIState
     *
     * If this call fails then will cancel onboarding and require the user to retry the flow with a link
     * If this call passes then the key creation and saving to cloud storage is triggered
     */
    private fun acceptApprovership() {
        viewModelScope.launch {
            val acceptResource = approverRepository.acceptApprovership(
                invitationId = state.invitationId,
            )

            state = state.copy(acceptApproverResource = acceptResource)

            if (acceptResource is Resource.Success) {
                state = state.copy(
                    onboardingMessage = Resource.Success(OnboardingMessage.LinkAccepted)
                )
                assignParticipantId(acceptResource.data.approverState)
                assignEntropy(acceptResource.data.approverState)
                createKeyAndTriggerCloudSave()
            }
        }
    }

    fun createKeyAndTriggerCloudSave() {
        //We allow user to retry this method, so we need to set loading here
        setLoadingUIState()

        val approverEncryptionKey = keyRepository.createApproverKey()

        state = state.copy(
            approverEncryptionKey = approverEncryptionKey,
            cloudStorageAction = CloudStorageActionData(
                triggerAction = true,
                action = CloudStorageActions.UPLOAD,
                reason = null
            ),
        )
    }

    private fun checkForPrivateKeyBeforeSubmittingVerificationCode() {
        //Setting WaitingForConfirmation so that the CodeEntry composable shows loading UI while we move forward
        state = state.copy(approverUIState = ApproverOnboardingUIState.WaitingForConfirmation)
        state.approverEncryptionKey?.let {
            submitVerificationCode()
        } ?: loadPrivateKeyFromCloud()
    }

    fun submitVerificationCode() {
        //If submitVerificationCode was called from view layer, then we need to set loading/WaitingForConfirmation UI state
        state = state.copy(approverUIState = ApproverOnboardingUIState.WaitingForConfirmation)

        viewModelScope.launch(Dispatchers.IO) {

            if (state.approverEncryptionKey == null) {
                loadPrivateKeyFromCloud()
                return@launch
            }

            if (state.invitationId.value.isEmpty()) {
                loadInvitationId()
            }

            val signedVerificationData = try {
                approverRepository.signVerificationCode(
                    verificationCode = state.verificationCode,
                    encryptionKey = state.approverEncryptionKey!!
                )
            } catch (e: Exception) {
                e.sendError(CrashReportingUtil.SubmitVerification)
                state = state.copy(
                    approverUIState = ApproverOnboardingUIState.CodeRejected,
                    submitVerificationResource = Resource.Error(exception = e),
                )
                return@launch
            }

            val submitVerificationResource = approverRepository.submitApproverVerification(
                invitationId = state.invitationId.value,
                submitApproverVerificationRequest = signedVerificationData
            )

            state = if (submitVerificationResource is Resource.Success) {
                determineApproverUIState(submitVerificationResource.data.approverState)
                state.copy(submitVerificationResource = submitVerificationResource)
            } else {
                state.copy(
                    submitVerificationResource = submitVerificationResource,
                    approverUIState = ApproverOnboardingUIState.CodeRejected,
                )
            }
        }
    }

    private fun loadPrivateKeyFromCloud() {
        state = state.copy(
            cloudStorageAction = CloudStorageActionData(
                triggerAction = true,
                action = CloudStorageActions.DOWNLOAD,
            ),
        )
    }

    private fun loadInvitationId() {
        state = state.copy(
            invitationId = InvitationId(approverRepository.retrieveInvitationId())
        )
    }

    fun triggerApproverEntranceNavigation() {
        state = state.copy(navToApproverEntrance = true)
    }

    private fun cancelOnboarding() {
        approverRepository.clearInvitationId()
        userStatePollingTimer.stop()

        state = state.copy(
            invitationId = InvitationId(""),
            navToApproverEntrance = true
        )
    }

    private fun shouldPollUserState(approverPhase: ApproverPhase?) =
        approverPhase is ApproverPhase.WaitingForCode ||
                approverPhase is ApproverPhase.WaitingForVerification ||
                approverPhase is ApproverPhase.VerificationRejected

    //endregion

    //region Cloud Storage
    fun getEncryptedKeyForUpload(): ByteArray? {
        val encryptionKey = state.approverEncryptionKey ?: return null
        val entropy = state.entropy ?: return null

        return encryptionKey.encryptWithEntropy(
            deviceKeyId = keyRepository.retrieveSavedDeviceId(),
            entropy = entropy
        )
    }

    fun handleCloudStorageActionSuccess(
        encryptedKey: ByteArray,
        cloudStorageAction: CloudStorageActions
    ) {
        state = state.copy(cloudStorageAction = CloudStorageActionData())

        val entropy = when (val approverPhase = state.approverState?.phase) {
            is ApproverPhase.VerificationRejected -> {
                approverPhase.entropy
            }

            is ApproverPhase.WaitingForCode -> {
                approverPhase.entropy
            }

            else -> null
        }

        assignEntropy(state.approverState)


        if (entropy == null) {
            retrieveApproverState(false)
            return
        }

        //Decrypt the byteArray
        val privateKey =
            encryptedKey.decryptWithEntropy(
                deviceKeyId = keyRepository.retrieveSavedDeviceId(),
                entropy = entropy
            )

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
            approverEncryptionKey = privateEncryptionKey,
            savePrivateKeyToCloudResource = Resource.Uninitialized
        )
        retrieveApproverState(false)
    }

    private fun keyDownloadSuccess(
        privateEncryptionKey: EncryptionKey,
    ) {
        state = state.copy(
            retrievePrivateKeyFromCloudResource = Resource.Uninitialized,
            approverEncryptionKey = privateEncryptionKey
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
        state = state.copy(
            savePrivateKeyToCloudResource = Resource.Error(exception = exception),
            approverUIState = ApproverOnboardingUIState.NeedsToSaveKey
        )
    }

    private fun keyDownloadFailure(exception: Exception?) {
        //Set error state for the submitVerificationCode resource
        state = state.copy(
            retrievePrivateKeyFromCloudResource = Resource.Uninitialized,
            submitVerificationResource = Resource.Error(
                exception = exception
            ),
            //If the user dismisses the error, then they will see the Enter code UI
            approverUIState = ApproverOnboardingUIState.NeedsToEnterCode

        )
    }
    //endregion

    //endregion

    //region Reset functions
    fun resetAcceptApproverResource() {
        state = state.copy(
            acceptApproverResource = Resource.Uninitialized
        )
        cancelOnboarding()
    }

    fun resetSubmitVerificationResource() {
        state = state.copy(
            submitVerificationResource = Resource.Uninitialized
        )
    }

    fun resetApproverEntranceNavigationTrigger() {
        state = state.copy(navToApproverEntrance = false)
    }

    fun resetMessage() {
        state = state.copy(onboardingMessage = Resource.Uninitialized)
    }

    fun resetNavigationResource() {
        state = state.copy(navigationResource = Resource.Uninitialized)
    }
    //endregion
}