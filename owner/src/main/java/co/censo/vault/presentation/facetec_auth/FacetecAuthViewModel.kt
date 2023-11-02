package co.censo.vault.presentation.facetec_auth

import android.graphics.Rect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.presentation.SharedColors
import co.censo.shared.util.projectLog
import co.censo.vault.R
import co.censo.vault.data.repository.FacetecRepository
import com.facetec.sdk.FaceTecCancelButtonCustomization
import com.facetec.sdk.FaceTecCustomization
import com.facetec.sdk.FaceTecFaceScanProcessor
import com.facetec.sdk.FaceTecFaceScanResultCallback
import com.facetec.sdk.FaceTecSessionResult
import com.facetec.sdk.FaceTecSessionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
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

    fun resetUserCanceled() {
        state = state.copy(userCancelled = Resource.Uninitialized)
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
            state = if (listOf(
                    FaceTecSessionStatus.USER_CANCELLED,
                    FaceTecSessionStatus.USER_CANCELLED_VIA_HARDWARE_BUTTON,
                    FaceTecSessionStatus.USER_CANCELLED_VIA_CLICKABLE_READY_SCREEN_SUBTEXT
            ).contains(sessionResult?.status)) {
                state.copy(
                    submitResultResponse = Resource.Uninitialized,
                    userCancelled = Resource.Success(Unit)
                )
            } else {
                state.copy(submitResultResponse = Resource.Error(exception = Exception(sessionResult?.status?.description())))
            }
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
                state = submitResultResponse.errorResponse?.errors?.firstOrNull()?.scanResultBlob?.let {
                    scanResultCallback?.proceedToNextStep(it.value)
                    state.copy(submitResultResponse = Resource.Uninitialized)
                } ?: run {
                    scanResultCallback?.cancel()
                    projectLog(message = "Failed to send data to backend")
                    state.copy(submitResultResponse = submitResultResponse)
                }
            }

        }
    }
    
    fun facetecCustomizations(): FaceTecCustomization {
        val customization = FaceTecCustomization()

        // Cancel Button
        customization.cancelButtonCustomization.customImage = R.drawable.close
        customization.cancelButtonCustomization.location = FaceTecCancelButtonCustomization.ButtonLocation.CUSTOM
        customization.cancelButtonCustomization.customLocation = Rect(20, 20, 25, 25)

        // no frame
        customization.frameCustomization.borderColor = Color.Transparent.toArgb()

        // no brand image
        customization.overlayCustomization.showBrandingImage = false

        customization.feedbackCustomization.textColor = Color.White.toArgb()
        customization.feedbackCustomization.backgroundColors = Color.Black.toArgb()
        customization.feedbackCustomization.cornerRadius = 20;

        // guidance screen
        customization.guidanceCustomization.buttonTextNormalColor = Color.White.toArgb()
        customization.guidanceCustomization.buttonBackgroundNormalColor = Color.Black.toArgb()
        customization.guidanceCustomization.buttonTextDisabledColor = Color.White.toArgb()
        customization.guidanceCustomization.buttonBackgroundDisabledColor = SharedColors.GreyText.toArgb()
        customization.guidanceCustomization.buttonTextHighlightColor = Color.White.toArgb()
        customization.guidanceCustomization.buttonBackgroundHighlightColor = Color.Black.copy(alpha = 0.9f).toArgb()
        customization.guidanceCustomization.cameraPermissionsScreenImage = R.drawable.camera
        customization.guidanceCustomization.foregroundColor = Color.Black.toArgb()
        customization.guidanceCustomization.retryScreenImageBorderColor = Color.Black.toArgb()

        customization.ovalCustomization.strokeColor = Color.Black.toArgb()
        customization.ovalCustomization.progressColor1 = Color.Black.toArgb()
        customization.ovalCustomization.progressColor2 = Color.Black.copy(alpha = 0.9f).toArgb()

        customization.resultScreenCustomization.foregroundColor = Color.Black.toArgb()
        customization.resultScreenCustomization.uploadProgressFillColor = Color.Black.toArgb()
        customization.resultScreenCustomization.uploadProgressTrackColor = Color.Black.copy(alpha = 0.5f).toArgb()
        customization.resultScreenCustomization.activityIndicatorColor = Color.Black.toArgb()
        customization.resultScreenCustomization.resultAnimationBackgroundColor = Color.Black.toArgb()
        customization.resultScreenCustomization.resultAnimationForegroundColor = Color.White.toArgb()

        return customization
    }

    fun FaceTecSessionStatus?.description(): String {
        return when (this) {
            FaceTecSessionStatus.LANDSCAPE_MODE_NOT_ALLOWED -> "Your device must be in portrait mode."
            FaceTecSessionStatus.CAMERA_INITIALIZATION_ISSUE -> "Your camera could not be started."
            FaceTecSessionStatus.CAMERA_PERMISSION_DENIED -> "You must enable the camera to continue."
            FaceTecSessionStatus.CONTEXT_SWITCH -> "Unable to complete, please do not leave the app during the face scan."
            FaceTecSessionStatus.ENCRYPTION_KEY_INVALID -> "Unable to continue - encryption key invalid."
            FaceTecSessionStatus.TIMEOUT -> "The face scan took too long, please try again."
            FaceTecSessionStatus.LOCKED_OUT -> "Too many face scan failures, please wait before trying again."
            FaceTecSessionStatus.MISSING_GUIDANCE_IMAGES -> "Unable to continue - missing guidance images."
            FaceTecSessionStatus.NON_PRODUCTION_MODE_KEY_INVALID -> "Unable to continue - non-production mode key invalid."
            FaceTecSessionStatus.NON_PRODUCTION_MODE_NETWORK_REQUIRED -> "Unable to continue - non-production mode network required."
            FaceTecSessionStatus.REVERSE_PORTRAIT_NOT_ALLOWED -> "Your device must be in portrait mode."
            FaceTecSessionStatus.SESSION_UNSUCCESSFUL,
            FaceTecSessionStatus.UNKNOWN_INTERNAL_ERROR -> "Unable to complete the face scan for an unknown reason, please try again."
            FaceTecSessionStatus.SESSION_COMPLETED_SUCCESSFULLY -> "The face scan was successful."
            FaceTecSessionStatus.USER_CANCELLED,
            FaceTecSessionStatus.USER_CANCELLED_VIA_HARDWARE_BUTTON,
            FaceTecSessionStatus.USER_CANCELLED_VIA_CLICKABLE_READY_SCREEN_SUBTEXT -> "The face scan was cancelled."
            null -> "Unexpected error"
        }
    }
}
