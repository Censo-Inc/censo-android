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
            state.initialPlanData.approverEncryptionKey == null -> createApproverKey()
            state.initialPlanData.createPolicyParams == null -> createPolicyParams()
            else -> startFacetec()
        }
    }


    fun determineUIStatus() {
        val uiStatus = when {
            state.initialPlanData.approverEncryptionKey == null -> InitialPlanSetupStep.CreateApproverKey
            state.initialPlanData.createPolicyParams == null -> InitialPlanSetupStep.CreatePolicyParams
            else -> InitialPlanSetupStep.Facetec
        }

        state = state.copy(initialPlanSetupStep = uiStatus)

        moveToNextAction()
    }

    private fun createApproverKey() {
        if (state.saveKeyToCloudResource is Resource.Loading) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            state = state.copy(saveKeyToCloudResource = Resource.Loading())
            try {
                val approverEncryptionKey = keyRepository.createGuardianKey()
                savePrivateKeyToCloud(approverEncryptionKey)
            } catch (e: Exception) {
                e.sendError(CrashReportingUtil.CreateApproverKey)
                state = state.copy(
                    saveKeyToCloudResource = Resource.Error(exception = e)
                )
            }
        }
    }

    private fun savePrivateKeyToCloud(approverEncryptionKey: EncryptionKey) {
        state = state.copy(
            initialPlanData = state.initialPlanData.copy(approverEncryptionKey = approverEncryptionKey),
            cloudStorageAction = CloudStorageActionData(
                triggerAction = true,
                action = CloudStorageActions.UPLOAD,
                reason = null
            )
        )
    }

    fun getEncryptedKeyForUpload() : ByteArray? {
        val entropy = retrieveEntropy() ?: return null

        val encryptionKey = state.initialPlanData.approverEncryptionKey ?: return null

        return encryptionKey.encryptWithEntropy(
            deviceKeyId = keyRepository.retrieveSavedDeviceId(),
            entropy = entropy
        )
    }

    fun onKeySaved(approverEncryptedKey: ByteArray) {
        val entropy = retrieveEntropy() ?: return

        //Decrypt the byteArray
        val privateKey =
            approverEncryptedKey.decryptWithEntropy(
                deviceKeyId = keyRepository.retrieveSavedDeviceId(),
                entropy = entropy
            )

        state = state.copy(
            initialPlanData = state.initialPlanData.copy(
                approverEncryptionKey = privateKey.toEncryptionKey()
            ),
            saveKeyToCloudResource = Resource.Uninitialized,
            cloudStorageAction = CloudStorageActionData()
        )
        determineUIStatus()
    }

    fun onKeySaveFailed(exception: Exception?) {
        state = state.copy(
            cloudStorageAction = CloudStorageActionData(),
            saveKeyToCloudResource = Resource.Error(exception = exception),
            initialPlanData = state.initialPlanData.copy(approverEncryptionKey = null),
        )
    }

    private fun createPolicyParams() {
        if (state.createPolicyParams is Resource.Loading) {
            return
        }
        viewModelScope.launch {
            state = state.copy(createPolicyParams = Resource.Loading())
            val createPolicyParams = ownerRepository.getCreatePolicyParams(
                Guardian.ProspectGuardian(
                    invitationId = InvitationId(""),
                    label = "Me",
                    participantId = state.participantId,
                    status = GuardianStatus.ImplicitlyOwner(
                        Base58EncodedGuardianPublicKey(state.initialPlanData.approverEncryptionKey!!.publicExternalRepresentation().value),
                        Clock.System.now()
                    )
                )
            )

            if (createPolicyParams is Resource.Success) {
                state = state.copy(
                    initialPlanData = state.initialPlanData.copy(
                        createPolicyParams = createPolicyParams.data
                    ),
                    createPolicyParams = createPolicyParams,
                )
                determineUIStatus()
            } else {
                state = state.copy(
                    createPolicyParams = createPolicyParams,
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

    suspend fun onPolicyCreationFaceScanReady(
        verificationId: BiometryVerificationId,
        facetecData: FacetecBiometry
    ): Resource<BiometryScanResultBlob> {
        state = state.copy(
            initialPlanSetupStep = InitialPlanSetupStep.PolicyCreation
        )

        return viewModelScope.async {
            val createPolicyResponse = ownerRepository.createPolicy(
                state.initialPlanData.createPolicyParams!!,
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
}