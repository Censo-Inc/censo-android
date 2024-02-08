package co.censo.censo.presentation.initial_plan_setup

import Base58EncodedApproverPublicKey
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
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.ApproverStatus
import co.censo.shared.data.model.InitialKeyData
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.data.storage.CloudStoragePermissionNotGrantedException
import co.censo.shared.presentation.cloud_storage.CloudAccessContract
import co.censo.shared.presentation.cloud_storage.CloudAccessState
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.projectLog
import co.censo.shared.util.sendError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

@HiltViewModel
class InitialPlanSetupViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository,
) : ViewModel(), CloudAccessContract {

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

    fun changeWelcomeStep(welcomeStep: WelcomeStep) {
        state = state.copy(welcomeStep = welcomeStep)
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
            state = state.copy(saveKeyToCloudResource = Resource.Loading)
            try {
                val approverEncryptionKey = keyRepository.createApproverKey()

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

                savePrivateKeyToCloud(encryptedKeyData = encryptedKey)
            } catch (e: Exception) {
                e.sendError(CrashReportingUtil.CreateApproverKey)
                state = state.copy(
                    saveKeyToCloudResource = Resource.Error(exception = e),
                    keyData = null
                )
            }
        }
    }

    override fun observeCloudAccessStateForAccessGranted(retryAction: () -> Unit) {
        viewModelScope.launch {
            keyRepository.collectCloudAccessState {
                when (it) {
                    CloudAccessState.AccessGranted -> {
                        retryAction()
                        //Stop collecting cloud access state
                        this.cancel()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun savePrivateKeyToCloud(
        encryptedKeyData: ByteArray,
        bypassScopeCheck: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val uploadResponse = try {
                keyRepository.saveKeyInCloud(
                    key = encryptedKeyData,
                    participantId = state.participantId,
                    bypassScopeCheck = bypassScopeCheck,
                )
            } catch (permissionNotGranted: CloudStoragePermissionNotGrantedException) {
                // TODO: Need to think about UI state at this step
                observeCloudAccessStateForAccessGranted {
                    //Retry this method
                    savePrivateKeyToCloud(
                        encryptedKeyData = encryptedKeyData,
                        bypassScopeCheck = true
                    )
                }
                return@launch
            }

            if (uploadResponse is Resource.Success) {
                state = state.copy(saveKeyToCloudResource = Resource.Uninitialized)
                determineUIStatus()
            } else if (uploadResponse is Resource.Error) {
                state = state.copy(
                    saveKeyToCloudResource = Resource.Error(exception = uploadResponse.exception),
                    keyData = null
                )
            }
        }
    }

    private fun loadPrivateKeyFromCloud(bypassScopeCheck: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val downloadResponse = try {
                keyRepository.retrieveKeyFromCloud(
                    participantId = state.participantId, bypassScopeCheck = bypassScopeCheck,
                )
            } catch (permissionNotGranted: CloudStoragePermissionNotGrantedException) {
                observeCloudAccessStateForAccessGranted {
                    //Retry this method
                    loadPrivateKeyFromCloud(bypassScopeCheck = true)
                }
                return@launch
            }

            if (downloadResponse is Resource.Success) {
                decryptKeyDataAndSetToState(downloadResponse.data)
            } else if (downloadResponse is Resource.Error) {
                downloadResponse.exception?.sendError(CrashReportingUtil.CloudDownload)
                resetLoadKeyStateAndCreateNewApproverKey()
            }
        }
    }

    private fun createPolicyParams() {
        if (state.createPolicyParamsResponse is Resource.Loading) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val keyData = state.keyData

            val hasKeySavedInCloud = try {
                keyRepository.userHasKeySavedInCloud(state.participantId)
            } catch (permissionNotGranted: CloudStoragePermissionNotGrantedException) {
                observeCloudAccessStateForAccessGranted {
                    //Retry this method
                    createPolicyParams()
                }
                return@launch
            } catch (e: Exception) {
                //TODO: Test this before Review
                state = state.copy(
                    createPolicyParamsResponse = Resource.Error(exception = Exception("Unable to check for saved key in cloud"))
                )
                return@launch
            }

            val publicKey = if (hasKeySavedInCloud) {
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

            state = state.copy(createPolicyParamsResponse = Resource.Loading)

            val entropy = retrieveEntropy()
            if (entropy == null) {
                state = state.copy(
                    createPolicyParamsResponse = Resource.Error(exception = Exception("Missing entropy when trying to secure data"))
                )
                return@launch
            }

            val deviceKeyId = keyRepository.retrieveSavedDeviceId()

            val createPolicyParams = ownerRepository.getCreatePolicyParams(
                Approver.ProspectApprover(
                    invitationId = InvitationId(""),
                    label = "Me",
                    participantId = state.participantId,
                    status = ApproverStatus.OwnerAsApprover(
                        entropy,
                        Clock.System.now()
                    )
                ),
                ownerApproverEncryptedPrivateKey = keyData.encryptedPrivateKey,
                ownerApproverKey = Base58EncodedApproverPublicKey(publicKey.value),
                deviceKeyId = deviceKeyId,
                entropy = entropy
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

    private fun retrieveEntropy(): Base64EncodedData? = (ownerRepository.getOwnerStateValue() as? OwnerState.Initial)?.entropy

    private fun startFacetec() {
        state = state.copy(initialPlanSetupStep = InitialPlanSetupStep.Facetec)
    }

    fun delayedReset() {
        viewModelScope.launch {
            delay(1000)
            state = InitialPlanSetupScreenState()
        }
    }

    fun resetError() {
        state = state.copy(
            createPolicyParamsResponse = Resource.Uninitialized,
            createPolicyResponse = Resource.Uninitialized,
            saveKeyToCloudResource = Resource.Uninitialized,
            loadKeyFromCloudResource = Resource.Uninitialized,
            deleteUserResource = Resource.Uninitialized
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
                ownerRepository.updateOwnerState(createPolicyResponse.data.ownerState)
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
            )
            determineUIStatus()
        } catch (e: Exception) {
            e.sendError(CrashReportingUtil.DecryptingKey)
            resetLoadKeyStateAndCreateNewApproverKey()
        }
    }

    private fun resetLoadKeyStateAndCreateNewApproverKey() {
        state = state.copy(
            loadKeyFromCloudResource = Resource.Uninitialized,
        )
        createApproverKey()
    }

    fun showDeleteUserDialog() {
        state = state.copy(triggerDeleteUserDialog = Resource.Success(Unit))
    }

    fun resetDeleteUserDialog() {
        state = state.copy(triggerDeleteUserDialog = Resource.Uninitialized)
    }

    fun deleteUser() {
        state = state.copy(
            deleteUserResource = Resource.Loading,
            triggerDeleteUserDialog = Resource.Uninitialized,
            initialPlanSetupStep = InitialPlanSetupStep.DeleteUser
        )

        viewModelScope.launch(Dispatchers.IO) {
            val response = ownerRepository.deleteUser(null)

            state = state.copy(
                deleteUserResource = response
            )

            if (response is Resource.Success) {
                state = state.copy(
                    kickUserOut = Resource.Success(Unit),
                )
            }
        }
    }
}