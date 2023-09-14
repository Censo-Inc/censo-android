package co.censo.vault.presentation.facetec_auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.vault.BuildConfig
import co.censo.shared.data.Resource
import co.censo.vault.data.repository.FacetecRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.vault.util.vaultLog
import com.facetec.sdk.FaceTecFaceScanProcessor
import com.facetec.sdk.FaceTecFaceScanResultCallback
import com.facetec.sdk.FaceTecSessionResult
import com.facetec.sdk.FaceTecSessionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Base64
import javax.inject.Inject

@HiltViewModel
class FacetecAuthViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val facetecRepository: FacetecRepository
) : ViewModel(), FaceTecFaceScanProcessor {

    var state by mutableStateOf(FacetecAuthState())
        private set

    fun onStart() {
        retrieveUserData()
    }

    private fun skipFacetec() {
        val contactId = state.userResponse.data?.contacts?.first()?.identifier ?: ""

        viewModelScope.launch {
            val facetecResponse = facetecRepository.submitResult(
                biometryId = state.facetecData?.id ?: "",
                faceScan = Base64.getEncoder().encodeToString(contactId.toByteArray()),
                auditTrailImage = Base64.getEncoder().encodeToString(contactId.toByteArray()),
                lowQualityAuditTrailImage = Base64.getEncoder().encodeToString(contactId.toByteArray()),
            )

            state = state.copy(submitResultResponse = facetecResponse)
        }
    }

    fun retry() {
        when {
            state.userResponse is Resource.Error -> {
                retrieveUserData()
            }
            state.initFacetecData is Resource.Error -> {
                retrieveFacetecData()
            }
            state.submitResultResponse is Resource.Error -> {
                facetecSDKInitialized()
            }
        }
    }

    private fun retrieveUserData() {
        viewModelScope.launch {
            state = state.copy(userResponse = Resource.Loading())
            val userResponse = ownerRepository.retrieveUser()
            state = state.copy(userResponse = userResponse)
        }
    }

    fun retrieveFacetecData() {
        viewModelScope.launch {
            state = state.copy(initFacetecData = Resource.Loading())
            val facetecDataResource = facetecRepository.startFacetecBiometry()

            if (facetecDataResource is Resource.Success) {
                state = state.copy(
                    initFacetecData = facetecDataResource,
                    facetecData = facetecDataResource.data,
                )
            } else if (facetecDataResource is Resource.Error) {
                state = state.copy(
                    initFacetecData = facetecDataResource
                )
            }
        }
    }

    fun facetecSDKInitialized() {
        if (BuildConfig.DEBUG) {
         skipFacetec()
        } else {
            state = state.copy(
                startAuth = Resource.Success(Unit),
                submitResultResponse = Resource.Loading()
            )
        }
    }

    fun failedToInitializeSDK() {
        vaultLog(message = "Failed to setup Facetec SDK")
        state = state.copy(initFacetecData = Resource.Error())
    }

    fun resetFacetecInitDataResource() {
        state = state.copy(initFacetecData = Resource.Uninitialized)
    }

    fun resetStartFacetecAuth() {
        state = state.copy(startAuth = Resource.Uninitialized)
    }

    fun resetSubmitResult() {
        state = state.copy(submitResultResponse = Resource.Uninitialized)
    }

    override fun processSessionWhileFaceTecSDKWaits(
        sessionResult: FaceTecSessionResult?,
        scanResultCallback: FaceTecFaceScanResultCallback?
    ) {
        if (sessionResult?.status != FaceTecSessionStatus.SESSION_COMPLETED_SUCCESSFULLY) {
            scanResultCallback?.cancel()
            state =
                state.copy(submitResultResponse = Resource.Error(exception = Exception("Facescan failed to complete. No result.")))
            return
        }

        vaultLog(message = "Successfully received facetec result...")
        state = state.copy(submitResultResponse = Resource.Loading())

        viewModelScope.launch {
            val submitResultResponse = facetecRepository.submitResult(
                state.facetecData?.id ?: "",
                sessionResult.faceScanBase64 ?: "",
                sessionResult.auditTrailCompressedBase64[0] ?: "",
                sessionResult.lowQualityAuditTrailCompressedBase64[0] ?: ""
            )

            if (submitResultResponse is Resource.Success) {
                vaultLog(message = "Success sending facetec data to backend")

                submitResultResponse.data?.scanResultBlob?.let {
                    scanResultCallback?.proceedToNextStep(it)
                } ?: scanResultCallback?.succeed()

            } else if (submitResultResponse is Resource.Error) {
                scanResultCallback?.cancel()
                vaultLog(message = "Failed to send data to backend")
            }

            state = state.copy(submitResultResponse = submitResultResponse)
        }
    }
}