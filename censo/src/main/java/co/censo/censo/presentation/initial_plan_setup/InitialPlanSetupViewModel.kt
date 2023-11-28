package co.censo.censo.presentation.initial_plan_setup

import Base58EncodedGuardianPublicKey
import InvitationId
import Base64EncodedData
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.decryptWithEntropy
import co.censo.shared.data.cryptography.encryptWithEntropy
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.presentation.cloud_storage.CloudStorageActionData
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.sendError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

@HiltViewModel
class InitialPlanSetupViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository,
    private val ownerStateFlow: MutableStateFlow<Resource<OwnerState>>
) : ViewModel() {

    var state by mutableStateOf(InitialPlanSetupScreenState())
        private set

    private fun moveToNextAction() {
        when {
            state.keyData?.encryptedPrivateKey == null -> createApproverKey()
            state.createPolicyParams == null -> createPolicyParams()
            else -> startFacetec()
        }
    }


    fun determineUIStatus() {
        val uiStatus = when {
            state.keyData?.encryptedPrivateKey  == null -> InitialPlanSetupStep.CreateApproverKey
            state.createPolicyParams == null -> InitialPlanSetupStep.CreatePolicyParams
            else -> InitialPlanSetupStep.Facetec
        }

        state = state.copy(initialPlanSetupStep = uiStatus)

        moveToNextAction()
    }

    private fun createApproverKey() {
        if (state.saveKeyToCloudResource is Resource.Loading) {
            return
        }

        val entropy = retrieveEntropy()

        if (entropy == null) {
            state = state.copy(
                saveKeyToCloudResource = Resource.Error(exception = Exception("Missing entropy when trying to save key"))
            )
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            state = state.copy(saveKeyToCloudResource = Resource.Loading())
            try {
                val approverEncryptionKey = keyRepository.createGuardianKey()

                val idToken = keyRepository.retrieveSavedDeviceId()

                val encryptedKey = approverEncryptionKey.encryptWithEntropy(
                    deviceKeyId = idToken,
                    entropy = entropy
                )

                val publicKey = approverEncryptionKey.publicExternalRepresentation()

                state = state.copy(
                    keyData = InitialKeyData(
                        encryptedPrivateKey = encryptedKey,
                        publicKey = publicKey
                    )
                )

                savePrivateKeyToCloud()
            } catch (e: Exception) {
                e.sendError(CrashReportingUtil.CreateApproverKey)
                state = state.copy(
                    saveKeyToCloudResource = Resource.Error(exception = e),
                    keyData = null
                )
            }
        }
    }

    private fun savePrivateKeyToCloud() {
        state = state.copy(
            cloudStorageAction = CloudStorageActionData(
                triggerAction = true,
                action = CloudStorageActions.UPLOAD,
                reason = null
            )
        )
    }

    fun onKeySaved() {
        state = state.copy(
            saveKeyToCloudResource = Resource.Uninitialized,
            cloudStorageAction = CloudStorageActionData()
        )
        determineUIStatus()
    }

    fun onKeySaveFailed(exception: Exception?) {
        state = state.copy(
            cloudStorageAction = CloudStorageActionData(),
            saveKeyToCloudResource = Resource.Error(exception = exception),
            keyData = null
        )
    }

    private fun loadPrivateKeyFromCloud() {
        state = state.copy(
            cloudStorageAction = CloudStorageActionData(
                triggerAction = true,
                action = CloudStorageActions.DOWNLOAD,
                reason = null
            )
        )
    }

    fun onKeyLoaded(encryptedData: ByteArray) {
        decryptKeyDataAndSetToState(encryptedData)
    }

    fun onKeyLoadFailed(exception: Exception?) {
        exception?.sendError(CrashReportingUtil.CloudDownload)
        resetLoadKeyStateAndCreateNewApproverKey()
    }

    private fun createPolicyParams() {
        if (state.createPolicyParamsResponse is Resource.Loading) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val publicKey = if (keyRepository.userHasKeySavedInCloud(state.participantId)) {
                val keyData = state.keyData
                if (keyData == null) {
                    loadPrivateKeyFromCloud()
                    return@launch
                } else {
                    keyData.publicKey
                }
            } else {
                createApproverKey()
                return@launch
            }

            state = state.copy(createPolicyParamsResponse = Resource.Loading())

            val createPolicyParams = ownerRepository.getCreatePolicyParams(
                Guardian.ProspectGuardian(
                    invitationId = InvitationId(""),
                    label = "Me",
                    participantId = state.participantId,
                    status = GuardianStatus.ImplicitlyOwner(
                        Base58EncodedGuardianPublicKey(publicKey.value),
                        Clock.System.now()
                    )
                )
            )

            if (createPolicyParams is Resource.Success) {
                state = state.copy(
                    createPolicyParams = createPolicyParams.data,
                    createPolicyParamsResponse = createPolicyParams,
                )
                determineUIStatus()
            } else {
                state = state.copy(
                    createPolicyParamsResponse = createPolicyParams,
                )
            }
        }
    }

    private fun retrieveEntropy(): Base64EncodedData? =
        (ownerStateFlow.value.data as? OwnerState.Initial)?.entropy

    private fun startFacetec() {
        state = state.copy(initialPlanSetupStep = InitialPlanSetupStep.Facetec)
    }

    fun reset() {
        state = InitialPlanSetupScreenState()
    }

    fun resetError() {
        state = state.copy(
            createPolicyParamsResponse = Resource.Uninitialized,
            createPolicyResponse = Resource.Uninitialized,
            saveKeyToCloudResource = Resource.Uninitialized,
            loadKeyFromCloudResource = Resource.Uninitialized
        )
    }

    suspend fun onPolicyCreationFaceScanReady(
        verificationId: BiometryVerificationId,
        facetecData: FacetecBiometry
    ): Resource<BiometryScanResultBlob> {
        state = state.copy(
            initialPlanSetupStep = InitialPlanSetupStep.PolicyCreation
        )

        return viewModelScope.async {
            val createPolicyResponse = ownerRepository.createPolicy(
                state.createPolicyParams!!,
                verificationId,
                facetecData
            )

            state = state.copy(
                createPolicyResponse = createPolicyResponse,
                complete = createPolicyResponse is Resource.Success
            )


            if (createPolicyResponse is Resource.Success) {
                ownerStateFlow.tryEmit(createPolicyResponse.map { it.ownerState })
            }

            createPolicyResponse.map { it.scanResultBlob }
        }.await()
    }

    private fun decryptKeyDataAndSetToState(encryptedKeyData: ByteArray) {
        try {
            val entropy = retrieveEntropy()
            if (entropy == null) {
                Exception("Missing entropy when trying to decrypt loaded key").sendError(CrashReportingUtil.DecryptingKey)
                resetLoadKeyStateAndCreateNewApproverKey()
                return
            }

            val base58EncodedPrivateKey = encryptedKeyData.decryptWithEntropy(
                deviceKeyId = keyRepository.retrieveSavedDeviceId(),
                entropy = entropy
            )

            val encryptionKey = EncryptionKey.generateFromPrivateKeyRaw(base58EncodedPrivateKey.bigInt())

            state = state.copy(
                keyData = InitialKeyData(
                    encryptedPrivateKey = encryptedKeyData,
                    publicKey = encryptionKey.publicExternalRepresentation()
                ),
                loadKeyFromCloudResource = Resource.Uninitialized,
                cloudStorageAction = CloudStorageActionData()
            )
            determineUIStatus()
        } catch (e: Exception) {
            e.sendError(CrashReportingUtil.DecryptingKey)
            resetLoadKeyStateAndCreateNewApproverKey()
        }
    }

    private fun resetLoadKeyStateAndCreateNewApproverKey() {
        state = state.copy(
            cloudStorageAction = CloudStorageActionData(),
            loadKeyFromCloudResource = Resource.Uninitialized,
        )
        createApproverKey()
    }
}