package co.censo.approver.presentation.auth_reset

import ParticipantId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.approver.presentation.home.EncryptedKey
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.cryptography.decryptWithEntropy
import co.censo.shared.data.model.ApproverPhase
import co.censo.shared.data.model.ApproverState
import co.censo.shared.data.model.AuthenticationResetApprovalId
import co.censo.shared.data.model.forParticipant
import co.censo.shared.data.repository.ApproverRepository
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.storage.CloudStoragePermissionNotGrantedException
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.isDigitsOnly
import co.censo.shared.util.observeCloudAccessStateForAccessGranted
import co.censo.shared.util.sendError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApproverAuthResetViewModel @Inject constructor(
    private val approverRepository: ApproverRepository,
    private val keyRepository: KeyRepository,
) : ViewModel() {

    var state by mutableStateOf(ApproverAuthResetState())
        private set

    fun onStart() {
        retrieveApproverState()
    }

    //region Retrieve user and determine UI state
    fun retrieveApproverState() {
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
        val participantId = ParticipantId(approverRepository.retrieveParticipantId())
        state = state.copy(approvalId = AuthenticationResetApprovalId(approverRepository.retrieveApprovalId()))

        // If participantId is empty go back to the paste link
        if (participantId.value.isEmpty()) {
            state = state.copy(navToApproverEntrance = true)
            return
        }

        when (val approverState =
            approverStates.forParticipant(participantId = participantId.value)) {
            null -> {
                approverRepository.clearParticipantId()
                state = state.copy(navToApproverEntrance = true)
            }

            else -> {
                state = state.copy(
                    participantId = participantId,
                    approverState = approverState
                )
                determineAuthResetUIState(approverState)
            }
        }
    }

    private fun determineAuthResetUIState(approverState: ApproverState) {
        state = when (val phase = approverState.phase) {

            is ApproverPhase.Complete -> {
                when (state.submitAuthResetVerificationResource) {
                    is Resource.Success -> {
                        approverRepository.clearApprovalId()
                        approverRepository.clearParticipantId()
                        state.copy(uiState = ApproverAuthResetState.UIState.Complete)
                    }
                    else -> {
                        state.copy(authResetNotInProgress = Resource.Error())
                    }
                }
            }

            is ApproverPhase.AuthenticationResetRequested ->
                state.copy(
                    uiState = ApproverAuthResetState.UIState.AuthenticationResetRequested,
                    approverState = approverState
                )

            is ApproverPhase.AuthenticationResetWaitingForCode -> {
                state.copy(
                    uiState = ApproverAuthResetState.UIState.NeedsToEnterCode,
                    approverState = approverState,
                    approverEntropy = phase.entropy,
                )
            }

            is ApproverPhase.AuthenticationResetVerificationRejected -> {
                state.copy(
                    uiState = ApproverAuthResetState.UIState.CodeRejected,
                    approverState = approverState,
                    approverEntropy = phase.entropy,
                    verificationCode = ""
                )
            }

            else -> state
        }
    }
    //endregion

    //region Accept request and submit signature
    fun acceptAuthResetRequest() {
        state = state.copy(acceptAuthResetResource = Resource.Loading)
        viewModelScope.launch {
            val acceptAuthResetRequest = approverRepository.acceptAuthenticationResetRequest(
                approvalId = state.approvalId
            )
            if (acceptAuthResetRequest is Resource.Success) {
                determineAuthResetUIState(
                    acceptAuthResetRequest.data.approverStates.forParticipant(
                        state.participantId.value
                    )!!
                )
            }
            state = state.copy(acceptAuthResetResource = acceptAuthResetRequest)
        }
    }

    fun checkAuthResetConfirmationPhase() {
        if (state.shouldAutoSubmitCode) {
            updateVerificationCode(state.verificationCode)
        } else {
            Exception().sendError(CrashReportingUtil.AuthRestConfirmation)
            state = state.copy(submitAuthResetVerificationResource = Resource.Error(exception = Exception("Unable to approve this access request, missing information")))
        }
    }

    fun updateVerificationCode(value: String) {
        if (state.submitAuthResetVerificationResource is Resource.Error) {
            state = state.copy(submitAuthResetVerificationResource = Resource.Uninitialized)
        }

        if (value.isDigitsOnly()) {
            state = state.copy(verificationCode = value)

            if (state.verificationCode.length == TotpGenerator.CODE_LENGTH) {
                checkForPrivateKeyBeforeSubmittingVerificationCode()
            }
        }
    }

    private fun checkForPrivateKeyBeforeSubmittingVerificationCode() {
        // setting WaitingForVerification so that the CodeEntry composable shows loading UI while we move forward with key download
        state = state.copy(uiState = ApproverAuthResetState.UIState.WaitingForVerification)
        state.approverEncryptedKey?.let {
            submitVerificationCode()
        } ?: loadPrivateKeyFromCloud()
    }

    private fun submitVerificationCode() {
        viewModelScope.launch(Dispatchers.IO) {

            val approverKey = state.approverEncryptedKey
            val entropy = state.approverEntropy

            if (approverKey == null) {
                loadPrivateKeyFromCloud()
                return@launch
            }

            if (entropy == null) {
                state = state.copy(submitAuthResetVerificationResource = Resource.Error(exception = Exception("Unable to approve this access request, missing information")))
                return@launch
            }

            val key = approverKey.key.decryptWithEntropy(
                deviceKeyId = keyRepository.retrieveSavedDeviceId(),
                entropy = entropy
            )

            val signedVerificationData = try {
                approverRepository.signVerificationCode(
                    verificationCode = state.verificationCode,
                    encryptionKey = key.toEncryptionKey()
                )
            } catch (e: Exception) {
                e.sendError(CrashReportingUtil.SubmitVerification)
                state = state.copy(
                    uiState = ApproverAuthResetState.UIState.CodeRejected,
                    submitAuthResetVerificationResource = Resource.Error(exception = e),
                )
                return@launch
            }

            val responseResource = approverRepository.submitAuthenticationResetTotpVerification(
                approvalId = state.approvalId,
                signature = signedVerificationData.signature,
                timeMillis = signedVerificationData.timeMillis
            )

            if (responseResource is Resource.Success) {
                state = state.copy(submitAuthResetVerificationResource = responseResource)
                determineAuthResetUIState(responseResource.data.approverStates.forParticipant(state.participantId.value)!!)
            } else {
                state = state.copy(
                    submitAuthResetVerificationResource = responseResource,
                    uiState = ApproverAuthResetState.UIState.CodeRejected,
                )
            }
        }
    }
    //endregion

    //region CloudStorage Action methods
    private fun loadPrivateKeyFromCloud(bypassScopeCheck: Boolean = false) {
        state = state.copy(loadKeyFromCloudResource = Resource.Loading)
        
        viewModelScope.launch(Dispatchers.IO) {
            val downloadResponse = try {
                keyRepository.retrieveKeyFromCloud(
                    participantId = state.participantId,
                    bypassScopeCheck = bypassScopeCheck,
                )
            } catch (permissionNotGranted: CloudStoragePermissionNotGrantedException) {
                observeCloudAccessStateForAccessGranted(
                    coroutineScope = this, keyRepository = keyRepository
                ) {
                    //Retry this method
                    loadPrivateKeyFromCloud(bypassScopeCheck = true)
                }
                return@launch
            }

            state = state.copy(loadKeyFromCloudResource = Resource.Uninitialized)

            if (downloadResponse is Resource.Success) {
                keyDownloadSuccess(privateEncryptedKey = downloadResponse.data)
            } else if (downloadResponse is Resource.Error) {
                setErrorToSubmitAuthResetVerificationResource(downloadResponse.exception)
            }
        }
    }

    private fun keyDownloadSuccess(privateEncryptedKey: ByteArray) {
        state = state.copy(approverEncryptedKey = EncryptedKey(privateEncryptedKey))

        checkAuthResetConfirmationPhase()
    }

    private fun setErrorToSubmitAuthResetVerificationResource(exception: Exception?) {
        state = state.copy(submitAuthResetVerificationResource = Resource.Error(
            exception = exception
        ))
    }
    //endregion

    //region Clear error state, handle exit
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
        cancelAuthReset()
    }

    private fun cancelAuthReset() {
        approverRepository.clearParticipantId()
        approverRepository.clearApprovalId()

        state = state.copy(
            approvalId = AuthenticationResetApprovalId(""),
            participantId = ParticipantId(""),
            navToApproverEntrance = true
        )
    }

    fun resetApproverEntranceNavigationTrigger() {
        state = state.copy(navToApproverEntrance = false)
    }

    fun resetSubmitAuthResetVerificationResource() {
        state = state.copy(
            submitAuthResetVerificationResource = Resource.Uninitialized
        )
    }

    fun resetAcceptAuthResetResource() {
        state = state.copy(acceptAuthResetResource = Resource.Uninitialized)
    }

    fun resetAuthResetNotInProgressResource() {
        cancelAuthReset()
        state = state.copy(acceptAuthResetResource = Resource.Uninitialized)
    }
    //endregion
}