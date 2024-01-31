package co.censo.censo.presentation.plan_finalization

import Base58EncodedApproverPublicKey
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.EncryptedShard
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.ApproverStatus
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.AccessIntent
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.censo.presentation.Screen
import co.censo.censo.presentation.plan_setup.PolicySetupAction
import co.censo.censo.util.TestUtil
import co.censo.censo.util.alternateApprover
import co.censo.censo.util.getEntropyFromOwnerApprover
import co.censo.censo.util.ownerApprover
import co.censo.censo.util.primaryApprover
import co.censo.shared.data.cryptography.decryptWithEntropy
import co.censo.shared.data.cryptography.encryptWithEntropy
import co.censo.shared.data.model.CompleteOwnerApprovershipApiRequest
import co.censo.shared.presentation.cloud_storage.CloudStorageActionData
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.sendError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 *
 * Main Processes:
 *
 * Process 1: Check for Owner Approver Key is saved to cloud and in state
 *      If no key saved to cloud
 *          Create key locally
 *          Encrypt it with entropy
 *          Save it to cloud
 *          Check owner user is finalized
 *              Once we key saved in the cloud, we need to upload
 *              the public key for the owner to backend.
 *      If key is saved to cloud
 *          Load key if it is not in local state
 *          Check owner user is finalized
 *
 * Process 2: Complete access to set new plan
 *      Complete facetec to get biometry data back
 *      Retrieve shards from backend
 *      Replace Policy
 *
 * Internal Methods
 *
 * updateOwnerState: Called anytime we get new owner state from backend.
 *      Sets all state data for the 3 approvers and ownerState
 *
 * initiateAccess: Finalized the plan setup, and need to do access to re-shard and finalize plan.
 *      API call to initiate access. If that is a success, send user to Facetec.
 *
 * faceScanReady: Face scan completed externally in FacetecAuth and we will now call replacePolicy
 *
 * replacePolicy: Replace existing policy, and finalize plan
 */


@HiltViewModel
class ReplacePolicyViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    var state by mutableStateOf(ReplacePolicyState())
        private set

    //region Events
    fun onCreate(policySetupAction: PolicySetupAction) {
        state = state.copy(policySetupAction = policySetupAction)

        val ownerState = ownerRepository.getOwnerStateValue()
        updateOwnerState(ownerState, nextAction = {
            checkUserHasSavedKeyAndSubmittedPolicy()
        })
    }

    fun receivePlanAction(action: ReplacePolicyAction) {
        when (action) {
            //Back Click
            ReplacePolicyAction.BackClicked -> onBackClicked()

            //Retry
            ReplacePolicyAction.Retry -> retrieveOwnerState(silent = false, nextAction = {
                checkUserHasSavedKeyAndSubmittedPolicy()
            })

            //Facetec Cancelled
            ReplacePolicyAction.FacetecCancelled -> onFacetecCancelled()

            //Cloud Actions
            ReplacePolicyAction.KeyUploadSuccess -> onKeyUploadSuccess()
            is ReplacePolicyAction.KeyDownloadSuccess -> onKeyDownloadSuccess(action.encryptedKey)

            is ReplacePolicyAction.KeyDownloadFailed -> onKeyDownloadFailed(action.e)
            is ReplacePolicyAction.KeyUploadFailed -> onKeyUploadFailed(action.e)

            //Finalizing Actions
            ReplacePolicyAction.Completed -> onFullyCompleted()
        }
    }
    //endregion

    //region Internal Methods
    private fun onBackClicked() {
        when (state.backArrowType) {
            ReplacePolicyState.BackIconType.NONE -> { }
            ReplacePolicyState.BackIconType.EXIT -> {
                state = state.copy(navigationResource = Resource.Success(Screen.OwnerVaultScreen.route))
            }
        }
    }

    private fun onFullyCompleted() {
        state = state.copy(navigationResource = Resource.Success(Screen.OwnerVaultScreen.route))
    }

    private fun retrieveOwnerState(silent: Boolean = false, nextAction: (() -> Unit)? = null) {
        if (!silent) {
            state = state.copy(userResponse = Resource.Loading)
        }
        viewModelScope.launch {
            val ownerStateResource = ownerRepository.retrieveUser().map { it.ownerState }

            ownerStateResource.onSuccess { updateOwnerState(it, nextAction) }

            state = state.copy(userResponse = ownerStateResource)
        }
    }

    private fun updateOwnerState(ownerState: OwnerState, nextAction: (() -> Unit)? = null) {
        if (ownerState !is OwnerState.Ready) return

        // update global state
        ownerRepository.updateOwnerState(ownerState)

        // figure out owner/primary/alternate approvers
        val approverSetup = ownerState.policySetup?.approvers ?: emptyList()
        val ownerApprover: Approver.ProspectApprover? = approverSetup.ownerApprover()
        val primaryApprover: Approver.ProspectApprover? = approverSetup.primaryApprover()
        val alternateApprover: Approver.ProspectApprover? = approverSetup.alternateApprover()

        state = state.copy(
            // approver names are needed on the last screen. Prevent resetting to 'null' after policy is replaced
            ownerApprover = ownerApprover ?: state.ownerApprover,
            primaryApprover = primaryApprover ?: state.primaryApprover,
            alternateApprover = alternateApprover ?: state.alternateApprover,
            ownerState = ownerState
        )

        nextAction?.invoke()
    }

    private fun saveKeyWithEntropy() {
        try {
            state = state.copy(saveKeyToCloud = Resource.Loading)
            val approverEncryptionKey = keyRepository.createApproverKey()

            val approverSetup = state.ownerState?.policySetup?.approvers ?: emptyList()
            val ownerApprover: Approver.ProspectApprover? = approverSetup.ownerApprover()

            val entropy = (ownerApprover?.status as? ApproverStatus.OwnerAsApprover)?.entropy!!

            val idToken = keyRepository.retrieveSavedDeviceId()

            val encryptedKey = approverEncryptionKey.encryptWithEntropy(
                deviceKeyId = idToken,
                entropy = entropy
            )

            val publicKey = Base58EncodedApproverPublicKey(
                approverEncryptionKey.publicExternalRepresentation().value
            )

            val keyData = ReplacePolicyKeyData(
                encryptedPrivateKey = encryptedKey,
                publicKey = publicKey
            )
            state = state.copy(
                keyData = keyData,
                cloudStorageAction = CloudStorageActionData(
                    triggerAction = true,
                    action = CloudStorageActions.UPLOAD,
                )
            )
        } catch (e: Exception) {
            state = state.copy(saveKeyToCloud = Resource.Error(exception = e))
        }
    }


    private fun completeApproverOwnership() {
        state = state.copy(completeApprovershipResponse = Resource.Loading)

        viewModelScope.launch {

            val completeOwnerApprovershipApiRequest =
                CompleteOwnerApprovershipApiRequest(
                    approverPublicKey = state.keyData?.publicKey!!
                )

            val approverSetup = state.ownerState?.policySetup?.approvers ?: emptyList()
            val ownerApprover: Approver.ProspectApprover? = approverSetup.ownerApprover()

            val partId = ownerApprover?.participantId!!

            val completeApprovershipResponse = ownerRepository.completeApproverOwnership(
                partId,
                completeOwnerApprovershipApiRequest
            )

            if (completeApprovershipResponse is Resource.Success) {
                updateOwnerState(completeApprovershipResponse.data.ownerState)
                initiateAccess()
            }

            state = state.copy(
                completeApprovershipResponse = completeApprovershipResponse
            )
        }
    }

    private fun checkUserHasSavedKeyAndSubmittedPolicy() {
        val owner = state.ownerApprover

        if (owner == null) {
            retrieveOwnerState()
            return
        }

        viewModelScope.launch(ioDispatcher) {
            //Check that the user has a key in local state or in the cloud before moving forward
            val loadedKey =
                state.keyData?.encryptedPrivateKey != null && state.keyData?.publicKey != null

            if (!loadedKey) {
                if (keyRepository.userHasKeySavedInCloud(owner.participantId)) {
                    state = state.copy(
                        cloudStorageAction = CloudStorageActionData(
                            triggerAction = true, action = CloudStorageActions.DOWNLOAD
                        ),
                    )
                } else {
                    saveKeyWithEntropy()
                }

                return@launch
            }

            if (owner.status is ApproverStatus.OwnerAsApprover) {
                initiateAccess()
                return@launch
            }

            completeApproverOwnership()
        }
    }

    private fun initiateAccess() {
        viewModelScope.launch {
            if (state.policySetupAction == PolicySetupAction.AddApprovers) {
                state = state.copy(initiateAccessResponse = Resource.Loading)

                // cancel previous access if exists
                state.ownerState?.access?.let {
                    ownerRepository.cancelAccess()
                }

                // request new access for policy replacement
                val initiateAccessResponse =
                    ownerRepository.initiateAccess(AccessIntent.ReplacePolicy)


                if (initiateAccessResponse is Resource.Success) {
                    // navigate to the facetec view
                    state = state.copy(replacePolicyUIState = ReplacePolicyUIState.AccessInProgress_2)

                    updateOwnerState(initiateAccessResponse.data.ownerState)
                }

                state = state.copy(initiateAccessResponse = initiateAccessResponse)
            } else {
                state = state.copy(replacePolicyUIState = ReplacePolicyUIState.AccessInProgress_2)
            }
        }
    }

    private fun replacePolicy(encryptedIntermediatePrivateKeyShards: List<EncryptedShard>) {
        state = state.copy(verifyKeyConfirmationSignature = Resource.Loading)

        try {

            if (state.ownerState!!.policySetup!!.approvers.any {
                    !ownerRepository.verifyKeyConfirmationSignature(it)
                }) {
                state = state.copy(verifyKeyConfirmationSignature = Resource.Error())
                return
            }

            state = state.copy(
                replacePolicyResponse = Resource.Loading,
                verifyKeyConfirmationSignature = Resource.Uninitialized
            )

            viewModelScope.launch(ioDispatcher) {

                val approverSetup = state.ownerState?.policySetup?.approvers ?: emptyList()
                val entropy = approverSetup.ownerApprover()?.getEntropyFromOwnerApprover()

                val ownerApproverKeyData = state.keyData
                val deviceKeyId = keyRepository.retrieveSavedDeviceId()

                if (entropy == null || ownerApproverKeyData == null) {
                    state = state.copy(
                        replacePolicyResponse =
                        Resource.Error(exception = Exception("Unable to setup data for secure storage"))
                    )
                    return@launch
                }

                val response = try {
                    ownerRepository.replacePolicy(
                        encryptedIntermediatePrivateKeyShards = encryptedIntermediatePrivateKeyShards,
                        encryptedMasterPrivateKey = state.ownerState!!.policy.encryptedMasterKey,
                        threshold = state.policySetupAction.threshold,
                        approvers = listOfNotNull(
                            state.ownerApprover,
                            state.primaryApprover,
                            state.alternateApprover
                        ),
                        ownerApproverEncryptedPrivateKey = ownerApproverKeyData.encryptedPrivateKey,
                        ownerApproverKey = ownerApproverKeyData.publicKey,
                        entropy = entropy,
                        deviceKeyId = deviceKeyId,
                    )
                } catch (e: Exception) {
                    e.sendError(CrashReportingUtil.CloudDownload)
                    state = state.copy(replacePolicyResponse = Resource.Error(exception = e))
                    return@launch
                }

                state = state.copy(replacePolicyResponse = response)

                if (response is Resource.Success) {
                    updateOwnerState(response.data.ownerState)
                    state = state.copy(replacePolicyUIState = ReplacePolicyUIState.Completed_3)
                }
            }
        } catch (e: Exception) {
            e.sendError(CrashReportingUtil.ReplacePolicy)
            state = state.copy(replacePolicyResponse = Resource.Error(exception = e))
        }
    }

    private fun onFacetecCancelled() {
        state = state.copy(navigationResource = Resource.Success(Screen.OwnerVaultScreen.route))
    }
    //endregion

    //region FaceScan
    suspend fun onFaceScanReady(
        verificationId: BiometryVerificationId,
        biometry: FacetecBiometry
    ): Resource<BiometryScanResultBlob> {
        state = state.copy(retrieveAccessShardsResponse = Resource.Loading)

        return viewModelScope.async {
            val retrieveShardsResponse = ownerRepository.retrieveAccessShards(verificationId, biometry)

            if (retrieveShardsResponse is Resource.Success) {
                ownerRepository.cancelAccess()

                replacePolicy(retrieveShardsResponse.data.encryptedShards)
            }

            state = state.copy(retrieveAccessShardsResponse = retrieveShardsResponse)

            retrieveShardsResponse.map { it.scanResultBlob }
        }.await()
    }
    //endregion

    //region Cloud Storage
    private fun onKeyUploadSuccess() {
        resetCloudStorageActionState()
        completeApproverOwnership()
    }

    private fun onKeyDownloadSuccess(encryptedKey: ByteArray) {
        resetCloudStorageActionState()

        try {
            val approverSetup = state.ownerState?.policySetup?.approvers ?: emptyList()
            val entropy = approverSetup.ownerApprover()?.getEntropyFromOwnerApprover()
            if (entropy == null) {
                val exception = Exception("Missing entropy when decrypting key")
                exception.sendError(CrashReportingUtil.ReplacePolicy)

                state = state.copy(
                    replacePolicyResponse = Resource.Error(exception = exception)
                )
                return
            }

            val deviceId = keyRepository.retrieveSavedDeviceId()

            val publicKey =
                Base58EncodedApproverPublicKey(
                    encryptedKey.decryptWithEntropy(
                        deviceKeyId = deviceId,
                        entropy = entropy
                    ).toEncryptionKey().publicExternalRepresentation().value
                )

            state = state.copy(
                keyData = ReplacePolicyKeyData(
                    encryptedPrivateKey = encryptedKey,
                    publicKey = publicKey
                )
            )

            checkUserHasSavedKeyAndSubmittedPolicy()
        } catch (exception: Exception) {
            exception.sendError(CrashReportingUtil.CloudDownload)
            state = state.copy(
                replacePolicyResponse = Resource.Error(exception = exception)
            )
        }
    }

    private fun onKeyUploadFailed(exception: Exception?) {
        state = state.copy(
            createPolicySetupResponse = Resource.Error(exception = exception),
            saveKeyToCloud = Resource.Uninitialized
        )
        exception?.sendError(CrashReportingUtil.CloudUpload)
    }

    private fun onKeyDownloadFailed(exception: Exception?) {
        state = state.copy(
            createPolicySetupResponse = Resource.Error(exception = exception),
            saveKeyToCloud = Resource.Uninitialized
        )
        exception?.sendError(CrashReportingUtil.CloudDownload)
    }
    //endregion

    //region Reset functions
    fun resetNavigationResource() {
        state = state.copy(navigationResource = Resource.Uninitialized)
    }

    private fun resetCloudStorageActionState() {
        state = state.copy(
            cloudStorageAction = CloudStorageActionData(),
            saveKeyToCloud = Resource.Uninitialized
        )
    }

    fun dismissCloudError() {
        //dismiss error
        state = state.copy(replacePolicyResponse = Resource.Uninitialized)
        //kick user to home
        state = state.copy(navigationResource = Resource.Success(Screen.OwnerVaultScreen.route))
        //ensure facetec doesn't block us from leaving
        state = state.copy(replacePolicyUIState = ReplacePolicyUIState.Uninitialized_1)
    }

    fun resetReplacePolicyResponse() {
        state = state.copy(replacePolicyResponse = Resource.Uninitialized)
    }

    fun resetInitiateAccessResponse() {
        state = state.copy(initiateAccessResponse = Resource.Uninitialized)
    }

    fun resetCreatePolicySetupResponse() {
        state = state.copy(createPolicySetupResponse = Resource.Uninitialized)
    }

    fun resetUserResponse() {
        state = state.copy(userResponse = Resource.Uninitialized)
    }

    fun resetVerifyKeyConfirmationSignature() {
        state = state.copy(verifyKeyConfirmationSignature = Resource.Uninitialized)
    }

    fun resetRetrieveAccessShardsResponse() {
        state = state.copy(retrieveAccessShardsResponse = Resource.Uninitialized)
    }
    //endregion

    //region UnitTest Helpers
    fun setUIState(uiState: ReplacePolicyUIState) {
        if (System.getProperty(TestUtil.TEST_MODE) == TestUtil.TEST_MODE_TRUE) {
            state = state.copy(replacePolicyUIState = uiState)
        }
    }
    //endregion
}
