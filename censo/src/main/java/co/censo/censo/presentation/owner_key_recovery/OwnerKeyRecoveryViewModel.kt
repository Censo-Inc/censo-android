package co.censo.censo.presentation.owner_key_recovery

import Base58EncodedApproverPublicKey
import Base64EncodedData
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.censo.presentation.Screen
import co.censo.censo.presentation.plan_finalization.ReplacePolicyKeyData
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.encryptWithEntropy
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.EncryptedShard
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.RetrieveAccessShardsApiResponse
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.data.storage.CloudStoragePermissionNotGrantedException
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.observeCloudAccessStateForAccessGranted
import co.censo.shared.util.sendError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OwnerKeyRecoveryViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository,
) : ViewModel() {

    var state by mutableStateOf(OwnerKeyRecoveryState())
        private set

    fun onCreate() {
        // Key recovery plan
        // 1. biometry
        // 2. retrieve shards (+ owner entropy and approver keys), recover keys
        // 3. validate approver keys signature
        // 4. upload new key to the cloud
        // 5. recreate and submit shards

        viewModelScope.launch {
            // owner state is always present since we just navigated from the access screen
            retrieveOwnerState()
        }
    }

    fun receiveAction(action: KeyRecoveryAction) {
        when (action) {
            KeyRecoveryAction.BackClicked -> onBackClicked()
            KeyRecoveryAction.Completed -> onCompleted()
            KeyRecoveryAction.Retry -> onRetry()
        }
    }

    private fun onRetry() {
        retrieveOwnerState()
    }

    private fun retrieveOwnerState() {
        updateOwnerState(ownerRepository.getOwnerStateValue())
    }

    private fun updateOwnerState(ownerState: OwnerState) {
        if (ownerState !is OwnerState.Ready) {
            // unexpected owner state
            state = state.copy(navigationResource = Resource.Success(Screen.EntranceRoute.route))
            return
        }

        state = state.copy(
            ownerParticipantId = ownerState.policy.approvers.find { it.isOwner }!!.participantId,
            ownerEntropy = ownerState.policy.ownerEntropy,
            encryptedMasterKey = ownerState.policy.encryptedMasterKey,
            ownerKeyUIState = OwnerKeyRecoveryUIState.AccessInProgress
        )
    }

    suspend fun onFaceScanReady(
        verificationId: BiometryVerificationId,
        biometry: FacetecBiometry
    ): Resource<BiometryScanResultBlob> {
        state = state.copy(retrieveAccessShardsResponse = Resource.Loading)

        return viewModelScope.async {
            val retrieveShardsResponse = ownerRepository.retrieveAccessShards(verificationId, biometry)
            state = state.copy(retrieveAccessShardsResponse = retrieveShardsResponse)

            if (retrieveShardsResponse is Resource.Success) {
                validateApproverKeysSignatureAndUploadNewKey(retrieveShardsResponse.data)
            }

            retrieveShardsResponse.map { it.scanResultBlob }
        }.await()
    }

    private fun validateApproverKeysSignatureAndUploadNewKey(shardsResponse: RetrieveAccessShardsApiResponse) {
        state = state.copy(verifyApproverKeysSignature = Resource.Loading)

        viewModelScope.launch(Dispatchers.IO) {
            val approverKeysSignatureByIntermediateKey = (shardsResponse.ownerState as? OwnerState.Ready)?.policy?.approverKeysSignatureByIntermediateKey

            try {
                val signatureValid = ownerRepository.verifyApproverPublicKeysSignature(
                    encryptedIntermediatePrivateKeyShards = shardsResponse.encryptedShards.filter { !it.isOwnerShard },
                    approverPublicKeys = shardsResponse.encryptedShards.mapNotNull { it.approverPublicKey },
                    approverPublicKeysSignature = approverKeysSignatureByIntermediateKey ?: Base64EncodedData("")
                )

                if (signatureValid) {
                    state = state.copy(verifyApproverKeysSignature = Resource.Success(Unit))

                    saveKeyWithEntropy(state.ownerEntropy!!)
                } else {
                    state = state.copy(verifyApproverKeysSignature = Resource.Error())
                }
            } catch (permissionNotGranted: CloudStoragePermissionNotGrantedException) {
                observeCloudAccessStateForAccessGranted(
                    coroutineScope = this, keyRepository = keyRepository
                ) {
                    validateApproverKeysSignatureAndUploadNewKey(shardsResponse = shardsResponse)
                }
                return@launch
            } catch (e: Exception) {
                state = state.copy(verifyApproverKeysSignature = Resource.Error(e))
                return@launch
            }
        }
    }

    private fun onKeyUploadSuccess() {
        resetCloudStorageActionState()

        replaceShards(
            // remove owner shard since owner can't access current key
            state.retrieveAccessShardsResponse.asSuccess().data.encryptedShards.filter { !it.isOwnerShard }
        )
    }

    private fun replaceShards(encryptedIntermediatePrivateKeyShards: List<EncryptedShard>) {
        try {
            state = state.copy(replaceShardsResponse = Resource.Loading)

            viewModelScope.launch(Dispatchers.IO) {
                ownerRepository.cancelAccess()

                val ownerApproverKeyData = state.keyData
                val ownerEntropy = state.ownerEntropy
                val deviceKeyId = keyRepository.retrieveSavedDeviceId()

                if (ownerEntropy == null || ownerApproverKeyData == null) {
                    state = state.copy(
                        replaceShardsResponse = Resource.Error(exception = Exception("Unable to setup data for secure storage"))
                    )
                    return@launch
                }

                val externalApproverKeys = encryptedIntermediatePrivateKeyShards.associate {
                    it.participantId to it.approverPublicKey!!
                }
                val ownerApproverKey = state.ownerParticipantId!! to state.keyData!!.publicKey

                val response = ownerRepository.replaceShards(
                    encryptedIntermediatePrivateKeyShards = encryptedIntermediatePrivateKeyShards,
                    encryptedMasterPrivateKey = state.encryptedMasterKey!!,
                    threshold = 2U,
                    approverPublicKeys = externalApproverKeys + ownerApproverKey,
                    ownerApproverEncryptedPrivateKey = ownerApproverKeyData.encryptedPrivateKey,
                    entropy = ownerEntropy,
                    deviceKeyId = deviceKeyId
                )

                state = state.copy(replaceShardsResponse = response)

                if (response is Resource.Success) {
                    state = state.copy(ownerKeyUIState = OwnerKeyRecoveryUIState.Completed)
                }
            }
        } catch (permissionNotGranted: CloudStoragePermissionNotGrantedException) {
            observeCloudAccessStateForAccessGranted(
                coroutineScope = viewModelScope, keyRepository = keyRepository
            ) {
                replaceShards(encryptedIntermediatePrivateKeyShards = encryptedIntermediatePrivateKeyShards)
            }
        } catch (e: Exception) {
            e.sendError(CrashReportingUtil.ReplacePolicyShards)
            state = state.copy(replaceShardsResponse = Resource.Error(exception = e))
        }
    }

    private fun saveKeyWithEntropy(ownerEntropy: Base64EncodedData) {
        try {
            state = state.copy(saveKeyToCloud = Resource.Loading)
            val approverEncryptionKey = keyRepository.createApproverKey()

            val idToken = keyRepository.retrieveSavedDeviceId()

            val encryptedKey = approverEncryptionKey.encryptWithEntropy(
                deviceKeyId = idToken,
                entropy = ownerEntropy
            )

            val publicKey = Base58EncodedApproverPublicKey(
                approverEncryptionKey.publicExternalRepresentation().value
            )

            state = state.copy(
                keyData = ReplacePolicyKeyData(
                    encryptedPrivateKey = encryptedKey,
                    publicKey = publicKey
                )
            )

            savePrivateKeyToCloud(encryptedKeyData = encryptedKey)
        } catch (e: Exception) {
            state = state.copy(saveKeyToCloud = Resource.Error(exception = e))
        }
    }

    private fun savePrivateKeyToCloud(
        encryptedKeyData: ByteArray,
        bypassScopeCheck: Boolean = false
    ) {
        val participantId = state.ownerParticipantId

        if (participantId == null) {
            onKeyUploadFailed(Exception("Unable to setup policy missing participant id"))
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val uploadResponse = try {
                keyRepository.saveKeyInCloud(
                    key = encryptedKeyData,
                    id = participantId.value,
                    bypassScopeCheck = bypassScopeCheck,
                )
            } catch (permissionNotGranted: CloudStoragePermissionNotGrantedException) {
                observeCloudAccessStateForAccessGranted(
                    coroutineScope = this, keyRepository = keyRepository
                ) {
                    savePrivateKeyToCloud(
                        encryptedKeyData = encryptedKeyData,
                        bypassScopeCheck = true
                    )
                }
                return@launch
            }

            if (uploadResponse is Resource.Success) {
                onKeyUploadSuccess()
            } else if (uploadResponse is Resource.Error) {
                onKeyUploadFailed(uploadResponse.exception)
            }
        }
    }

    private fun onBackClicked() {
        state = state.copy(navigationResource = Resource.Success(Screen.OwnerVaultScreen.route))
    }

    private fun onKeyUploadFailed(exception: Exception?) {
        state = state.copy(
            replaceShardsResponse = Resource.Error(exception = exception),
            saveKeyToCloud = Resource.Uninitialized
        )
        exception?.sendError(CrashReportingUtil.CloudUpload)
    }

    private fun resetCloudStorageActionState() {
        state = state.copy(saveKeyToCloud = Resource.Uninitialized)
    }

    fun resetNavigationResource() {
        state = state.copy(navigationResource =  Resource.Uninitialized)
    }

    private fun onCompleted() {
        state = state.copy(navigationResource = Resource.Success(Screen.EntranceRoute.route))
    }

    fun resetRetrieveAccessShardsResponse() {
        state = state.copy(retrieveAccessShardsResponse = Resource.Uninitialized)
    }

    fun dismissCloudError() {
        state = state.copy(replaceShardsResponse = Resource.Uninitialized)

    }

    fun resetReplaceShardsResponse() {
        state = state.copy(replaceShardsResponse = Resource.Uninitialized)
    }

    fun resetVerifyApproverKeysSignature() {
        state = state.copy(verifyApproverKeysSignature = Resource.Uninitialized)
    }

}
