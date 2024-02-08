package co.censo.censo.presentation.beneficiary

import Base58EncodedBeneficiaryPublicKey
import Base64EncodedData
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.TotpGenerator
import co.censo.shared.data.cryptography.decryptWithEntropy
import co.censo.shared.data.cryptography.encryptWithEntropy
import co.censo.shared.data.cryptography.generateVerificationCodeSignData
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.model.BeneficiaryPhase
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.SubmitBeneficiaryVerificationApiRequest
import co.censo.shared.data.repository.BeneficiaryRepository
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.presentation.cloud_storage.CloudStorageActionData
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.util.CountDownTimerImpl
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.VaultCountDownTimer
import co.censo.shared.util.isDigitsOnly
import co.censo.shared.util.sendError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.bouncycastle.util.encoders.Base64
import javax.inject.Inject

/**
 *
 * onStart:
 *      Retrieve user from in memory and look at beneficiary data in OwnerState.Beneficiary
 *      Create key if there is no key locally or in cloud
 *      Load key if there is a key in cloud but not in memory
 *      Do polling to check if verification was accepted
 *
 * Send in code to verify beneficiary with owner
 *      If no key has been made, then we make it, save to cloud and submit verification
 *      If key has been made, but we do not have it locally, then we retrieve it, and then we submit verification
 *      If key has been made and we have it locally, we submit verification
 *
 */

@HiltViewModel
class BeneficiaryViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository,
    private val beneficiaryRepository: BeneficiaryRepository,
    private val userStatePollingTimer: VaultCountDownTimer
) : ViewModel() {

    var state by mutableStateOf(BeneficiaryState())
        private set

    fun onStart() {
        val ownerValue = ownerRepository.getOwnerStateValue()

        (ownerValue as? OwnerState.Beneficiary)?.let {
            state = state.copy(beneficiaryData = it)
        }

        createKeyOrLoadKeyIntoMemory()

        retrieveOwnerUser()

        userStatePollingTimer.start(CountDownTimerImpl.Companion.POLLING_VERIFICATION_COUNTDOWN) {
            if (state.apiResponse !is Resource.Loading
                && state.savePrivateKeyToCloudResource !is Resource.Loading
                && (shouldPollUserState(state.beneficiaryData?.phase))
            ) {
                retrieveOwnerUser()
            }
        }
    }

    private fun retrieveOwnerUser() {
        viewModelScope.launch {
            val ownerUser = ownerRepository.retrieveUser().map { it.ownerState }

            ownerUser.onSuccess { updateOwnerState(it) }
        }
    }

    private fun updateOwnerState(ownerState: OwnerState) {
        if (ownerState !is OwnerState.Beneficiary) {
            return
        }

        state = state.copy(beneficiaryData = ownerState)
    }

    fun onStop() {
        userStatePollingTimer.stopWithDelay(CountDownTimerImpl.Companion.VERIFICATION_STOP_DELAY)
    }

    private fun createKeyAndTriggerCloudSave() {
        val beneficiaryEncryptionKey = keyRepository.createApproverKey()

        state = state.copy(
            beneficiaryEncryptionKey = beneficiaryEncryptionKey,
            cloudStorageAction = CloudStorageActionData(
                triggerAction = true,
                action = CloudStorageActions.UPLOAD,
                reason = null
            )
        )
    }

    fun updateVerificationCode(value: String) {
        if (state.apiResponse is Resource.Error) {
            state = state.copy(apiResponse = Resource.Uninitialized)
        }

        if (value.isDigitsOnly()) {
            state = state.copy(verificationCode = value)

            if (state.verificationCode.length == TotpGenerator.CODE_LENGTH) {
                checkForPrivateKeyBeforeSubmittingVerificationCode()
            }
        }
    }

    private fun shouldPollUserState(beneficiaryPhase: BeneficiaryPhase?) =
        beneficiaryPhase is BeneficiaryPhase.WaitingForVerification ||
                beneficiaryPhase is BeneficiaryPhase.VerificationRejected

    private fun checkForPrivateKeyBeforeSubmittingVerificationCode() {
        state = state.copy(manuallyLoadingVerificationCode = true)

        state.beneficiaryEncryptionKey?.let {
            submitVerificationCode()
        } ?: loadPrivateKeyFromCloud()
    }

    private fun submitVerificationCode() {
        viewModelScope.launch(Dispatchers.IO) {

            if (state.beneficiaryEncryptionKey == null) {
                createKeyOrLoadKeyIntoMemory()
                return@launch
            }

            val invitationId = state.beneficiaryData?.invitationId?.value

            if (invitationId == null) {
                state = state.copy(
                    error = BeneficiaryError.MissingInviteCode,
                    manuallyLoadingVerificationCode = false
                )
                return@launch
            }

            val signedVerificationData = try {
                signVerificationCode(
                    verificationCode = state.verificationCode,
                    encryptionKey = state.beneficiaryEncryptionKey!!
                )
            } catch (e: Exception) {
                e.sendError(CrashReportingUtil.SubmitVerification)
                state = state.copy(
                    error = BeneficiaryError.FailedSubmitCode,
                    manuallyLoadingVerificationCode = false
                )
                return@launch
            }

            val submitVerificationResource = beneficiaryRepository.submitBeneficiaryVerification(
                invitationId = invitationId,
                apiRequest = signedVerificationData
            )

            submitVerificationResource.onSuccess {
                updateOwnerState(it.ownerState)
            }

            state = state.copy(
                apiResponse = submitVerificationResource,
                manuallyLoadingVerificationCode = false
            )
        }
    }

    private fun signVerificationCode(
        verificationCode: String,
        encryptionKey: EncryptionKey
    ): SubmitBeneficiaryVerificationApiRequest {
        val currentTimeInMillis = Clock.System.now().toEpochMilliseconds()
        val dataToSign =
            verificationCode.generateVerificationCodeSignData(currentTimeInMillis)
        val signature = encryptionKey.sign(dataToSign)
        val base64EncodedData =
            Base64EncodedData(Base64.toBase64String(signature))

        return SubmitBeneficiaryVerificationApiRequest(
            signature = base64EncodedData,
            timeMillis = currentTimeInMillis,
            beneficiaryPublicKey = Base58EncodedBeneficiaryPublicKey(
                encryptionKey.publicExternalRepresentation().value
            )
        )
    }

    fun resetErrorState() {
        state = state.copy(error = null)
    }

    fun inviteId() = state.beneficiaryData?.invitationId?.value ?: ""

    private fun createKeyOrLoadKeyIntoMemory() {
        if (state.beneficiaryEncryptionKey == null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    if (keyRepository.userHasKeySavedInCloud(inviteId())) {
                        loadPrivateKeyFromCloud()
                    } else {
                        createKeyAndTriggerCloudSave()
                    }
                } catch (e: NoSuchElementException) {
                    createKeyAndTriggerCloudSave()
                }
            }
        }
    }

    //region Cloud Storage
    private fun loadPrivateKeyFromCloud() {
        state = state.copy(
            cloudStorageAction = CloudStorageActionData(
                triggerAction = true,
                action = CloudStorageActions.DOWNLOAD,
            ),
        )
    }
    fun getEncryptedKeyForUpload(): ByteArray? {
        val encryptionKey = state.beneficiaryEncryptionKey ?: return null

        return encryptionKey.encryptWithEntropy(
            deviceKeyId = keyRepository.retrieveSavedDeviceId(),
            entropy = state.beneficiaryData?.entropy!!
        )
    }

    fun handleCloudStorageActionSuccess(
        encryptedKey: ByteArray,
        cloudStorageAction: CloudStorageActions
    ) {
        state = state.copy(cloudStorageAction = CloudStorageActionData())

        val entropy = state.beneficiaryData?.entropy!!

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
            beneficiaryEncryptionKey = privateEncryptionKey,
            savePrivateKeyToCloudResource = Resource.Uninitialized
        )
        state.beneficiaryData?.let { updateOwnerState(it) } ?: retrieveOwnerUser()
    }

    private fun keyDownloadSuccess(
        privateEncryptionKey: EncryptionKey,
    ) {
        state = state.copy(
            retrievePrivateKeyFromCloudResource = Resource.Uninitialized,
            beneficiaryEncryptionKey = privateEncryptionKey
        )
        state.beneficiaryData?.let { updateOwnerState(it) } ?: retrieveOwnerUser()
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
        exception?.sendError(CrashReportingUtil.CloudUpload)
        state = state.copy(
            savePrivateKeyToCloudResource = Resource.Error(exception = exception),
            error = BeneficiaryError.CloudError
        )
    }

    private fun keyDownloadFailure(exception: Exception?) {
        exception?.sendError(CrashReportingUtil.CloudDownload)
        state = state.copy(
            retrievePrivateKeyFromCloudResource = Resource.Uninitialized,
            apiResponse = Resource.Error(
                exception = exception
            ),
            error = BeneficiaryError.CloudError
        )
    }
    //endregion
    //endregion
}