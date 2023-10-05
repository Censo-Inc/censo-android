package co.censo.vault.presentation.facetec_auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.vault.BuildConfig
import co.censo.shared.data.Resource
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.util.projectLog
import co.censo.vault.data.repository.FacetecRepository
import com.facetec.sdk.FaceTecFaceScanProcessor
import com.facetec.sdk.FaceTecFaceScanResultCallback
import com.facetec.sdk.FaceTecSessionResult
import com.facetec.sdk.FaceTecSessionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Base64
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class FacetecAuthViewModel @Inject constructor(
    private val facetecRepository: FacetecRepository
) : ViewModel(), FaceTecFaceScanProcessor {

    var state by mutableStateOf(FacetecAuthState())
        private set

    private lateinit var onFaceScanReady: suspend (BiometryVerificationId, FacetecBiometry) -> Resource<BiometryScanResultBlob>

    fun onStart(
        onFaceScanReady: suspend (BiometryVerificationId, FacetecBiometry) -> Resource<BiometryScanResultBlob>
    ) {
        this.onFaceScanReady = onFaceScanReady
        initFacetecSession()
    }

    private fun skipFacetec() {
        val identityToken = UUID.randomUUID().toString().toByteArray()

        viewModelScope.launch {
            state = state.copy(
                submitResultResponse = onFaceScanReady(
                    state.facetecData?.id ?: BiometryVerificationId(""),
                    FacetecBiometry(
                        faceScan = Base64.getEncoder().encodeToString(identityToken),
                        auditTrailImage = Base64.getEncoder().encodeToString(identityToken),
                        lowQualityAuditTrailImage = Base64.getEncoder()
                            .encodeToString(identityToken),
                    )
                )
            )
        }
    }

    fun retry() {
        when {
            state.initFacetecData is Resource.Error -> {
                initFacetecSession()
            }
            state.submitResultResponse is Resource.Error -> {
                facetecSDKInitialized()
            }
        }
    }

    fun initFacetecSession() {
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
        state = state.copy(
            startAuth = Resource.Success(Unit),
            submitResultResponse = Resource.Loading()
        )
    }

    fun failedToInitializeSDK() {
        projectLog(message = "Failed to setup Facetec SDK")
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

        projectLog(message = "Successfully received facetec result...")
        state = state.copy(submitResultResponse = Resource.Loading())

        viewModelScope.launch {
            val submitResultResponse = onFaceScanReady(
                    state.facetecData?.id ?: BiometryVerificationId(""),
                    FacetecBiometry(
                        sessionResult.faceScanBase64 ?: "",
                        sessionResult.auditTrailCompressedBase64[0] ?: "",
                        sessionResult.lowQualityAuditTrailCompressedBase64[0] ?: ""
                    )
                )

            if (submitResultResponse is Resource.Success) {
                projectLog(message = "Success sending facetec data to backend")

                submitResultResponse.data?.value?.let {
                    scanResultCallback?.proceedToNextStep(it)
                } ?: scanResultCallback?.succeed()

                state = state.copy(submitResultResponse = Resource.Uninitialized)

            } else if (submitResultResponse is Resource.Error) {
                scanResultCallback?.cancel()
                projectLog(message = "Failed to send data to backend")
                state = state.copy(submitResultResponse = submitResultResponse)
            }

        }
    }
}