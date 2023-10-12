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
import co.censo.shared.data.model.CreatePolicyApiResponse
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
        state.copy(
            initialPlanSetupStatus = InitialPlanSetupScreenState.InitialPlanSetupStatus.Initial
        )
        createApproverKey()
    }

    fun createApproverKey() {
        viewModelScope.launch {
            state = try {
                val approverEncryptionKey = keyRepository.createGuardianKey()
                keyRepository.saveKeyInCloud(
                    Base58EncodedPrivateKey(
                        Base58.base58Encode(
                            approverEncryptionKey.privateKeyRaw()
                        )
                    )
                )
                state.copy(
                    approverEncryptionKey = approverEncryptionKey,
                    initialPlanSetupStatus = InitialPlanSetupScreenState.InitialPlanSetupStatus.Initial
                )
            } catch (e: Exception) {
                state.copy(
                    initialPlanSetupStatus = InitialPlanSetupScreenState.InitialPlanSetupStatus.ApproverKeyCreationFailed
                )
            }
        }
    }

    fun startPolicySetup() {
        state = state.copy(
            initialPlanSetupStatus = InitialPlanSetupScreenState.InitialPlanSetupStatus.SetupInProgress(
                apiCall = Resource.Uninitialized
            )
        )
    }

    fun resetComplete() {
        state = state.copy(
            initialPlanSetupStatus = InitialPlanSetupScreenState.InitialPlanSetupStatus.None,
            approverEncryptionKey = null,
            complete = false
        )
    }

    fun reset() {
        state = state.copy(
            initialPlanSetupStatus = InitialPlanSetupScreenState.InitialPlanSetupStatus.Initial,
            complete = false
        )
    }


    suspend fun onPolicySetupCreationFaceScanReady(
        verificationId: BiometryVerificationId,
        facetecData: FacetecBiometry
    ): Resource<BiometryScanResultBlob> {
        state = state.copy(
            initialPlanSetupStatus = InitialPlanSetupScreenState.InitialPlanSetupStatus.SetupInProgress(
                apiCall = Resource.Loading()
            )
        )

        return viewModelScope.async {
            val createPolicySetupResponse = ownerRepository.createPolicySetup(
                1U,
                listOf(
                    Guardian.SetupGuardian.ImplicitlyOwner(
                        label = "Me",
                        participantId = state.participantId,
                        guardianPublicKey = Base58EncodedGuardianPublicKey(
                            state.approverEncryptionKey!!.publicExternalRepresentation().value
                        )
                    )
                ),
                verificationId,
                facetecData
            )

            if (createPolicySetupResponse is Resource.Success) {
                createPolicy()
            } else {
                state = state.copy(
                    initialPlanSetupStatus = InitialPlanSetupScreenState.InitialPlanSetupStatus.SetupInProgress(
                        apiCall = createPolicySetupResponse
                    )
                )
            }

            createPolicySetupResponse.map { it.scanResultBlob }
        }.await()
    }

    fun createPolicy() {
        state = state.copy(
            initialPlanSetupStatus = InitialPlanSetupScreenState.InitialPlanSetupStatus.CreateInProgress(
                apiCall = Resource.Loading()
            )
        )

        viewModelScope.launch {
            val createPolicyResponse: Resource<CreatePolicyApiResponse> =
                ownerRepository.createPolicy(
                    1u,
                    listOf(
                        Guardian.ProspectGuardian(
                            label = "Me",
                            participantId = state.participantId,
                            status = GuardianStatus.ImplicitlyOwner(
                                Base58EncodedGuardianPublicKey(state.approverEncryptionKey!!.publicExternalRepresentation().value),
                                Clock.System.now()
                            )
                        )
                    )
                )
            state = state.copy(
                initialPlanSetupStatus = InitialPlanSetupScreenState.InitialPlanSetupStatus.CreateInProgress(
                    apiCall = createPolicyResponse
                ),
                complete = createPolicyResponse is Resource.Success
            )
        }
    }
}