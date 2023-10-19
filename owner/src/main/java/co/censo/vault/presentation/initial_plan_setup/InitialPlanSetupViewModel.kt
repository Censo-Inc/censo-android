package co.censo.vault.presentation.initial_plan_setup

import Base58EncodedGuardianPublicKey
import Base58EncodedPrivateKey
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.GuardianStatus
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.novacrypto.base58.Base58
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

@HiltViewModel
class InitialPlanSetupViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository,
) : ViewModel() {

    var state by mutableStateOf(InitialPlanSetupScreenState())
        private set

    fun onStart() {
        state = state.copy(
            initialPlanSetupStatus = InitialPlanSetupScreenState.InitialPlanSetupStatus.Initial
        )
        createApproverKeyAndPolicyParams()
    }

    fun createApproverKeyAndPolicyParams() {
        createApproverKey()
        if (state.approverEncryptionKey != null) {
            createPolicyParams()
        }
    }

    private fun createApproverKey() {
        if (state.saveKeyToCloudResource is Resource.Loading) {
            return
        }
        viewModelScope.launch {
            state = state.copy(saveKeyToCloudResource = Resource.Loading())
            state = try {
                val approverEncryptionKey = keyRepository.createGuardianKey()
                keyRepository.saveKeyInCloud(
                    key = Base58EncodedPrivateKey(
                        Base58.base58Encode(
                            approverEncryptionKey.privateKeyRaw()
                        )
                    ),
                    participantId = state.participantId
                )
                state.copy(
                    approverEncryptionKey = approverEncryptionKey,
                    initialPlanSetupStatus = InitialPlanSetupScreenState.InitialPlanSetupStatus.CreatingPolicyParams,
                    saveKeyToCloudResource = Resource.Uninitialized,
                )
            } catch (e: Exception) {
                state.copy(
                    initialPlanSetupStatus = InitialPlanSetupScreenState.InitialPlanSetupStatus.ApproverKeyCreationFailed,
                    saveKeyToCloudResource = Resource.Uninitialized
                )
            }
        }
    }

    fun createPolicyParams() {
        if (state.createPolicyParams is Resource.Loading) {
            return
        }
        viewModelScope.launch {
            state = state.copy(createPolicyParams = Resource.Loading())
            val createPolicyParams = ownerRepository.getCreatePolicyParams(
                Guardian.ProspectGuardian(
                    label = "Me",
                    participantId = state.participantId,
                    status = GuardianStatus.ImplicitlyOwner(
                        Base58EncodedGuardianPublicKey(state.approverEncryptionKey!!.publicExternalRepresentation().value),
                        Clock.System.now()
                    )
                )
            )
            state = state.copy(
                createPolicyParams = createPolicyParams,
                initialPlanSetupStatus = if (createPolicyParams is Resource.Success) InitialPlanSetupScreenState.InitialPlanSetupStatus.Initial else InitialPlanSetupScreenState.InitialPlanSetupStatus.CreatePolicyParamsFailed
            )
        }
    }

    fun startPolicySetup() {
        state = state.copy(
            initialPlanSetupStatus = InitialPlanSetupScreenState.InitialPlanSetupStatus.CreateInProgress(
                apiCall = Resource.Uninitialized
            )
        )
    }

    fun resetComplete() {
        state = state.copy(
            initialPlanSetupStatus = InitialPlanSetupScreenState.InitialPlanSetupStatus.None,
            approverEncryptionKey = null,
            complete = false,
            createPolicyParams = Resource.Uninitialized
        )
    }

    fun reset() {
        state = state.copy(
            initialPlanSetupStatus = InitialPlanSetupScreenState.InitialPlanSetupStatus.Initial,
            complete = false
        )
    }


    suspend fun onPolicyCreationFaceScanReady(
        verificationId: BiometryVerificationId,
        facetecData: FacetecBiometry
    ): Resource<BiometryScanResultBlob> {
        state = state.copy(
            initialPlanSetupStatus = InitialPlanSetupScreenState.InitialPlanSetupStatus.CreateInProgress(
                apiCall = Resource.Loading()
            )
        )

        return viewModelScope.async {
            val createPolicyResponse = ownerRepository.createPolicy(
                state.createPolicyParams.data!!,
                verificationId,
                facetecData
            )

            state = state.copy(
                initialPlanSetupStatus = InitialPlanSetupScreenState.InitialPlanSetupStatus.CreateInProgress(
                    apiCall = createPolicyResponse
                ),
                complete = createPolicyResponse is Resource.Success
            )

            createPolicyResponse.map { it.scanResultBlob }
        }.await()
    }
}