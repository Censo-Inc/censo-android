package co.censo.censo.presentation.facetec_auth

import StandardButton
import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import co.censo.censo.BuildConfig
import co.censo.shared.data.Resource
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.FacetecBiometry
import co.censo.censo.R
import co.censo.shared.presentation.components.LargeLoading
import com.facetec.sdk.FaceTecSDK
import com.facetec.sdk.FaceTecSessionActivity


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun FacetecAuth(
    onFaceScanReady: suspend (BiometryVerificationId, FacetecBiometry) -> Resource<BiometryScanResultBlob>,
    onCancelled: () -> Unit = {},
    viewModel: FacetecAuthViewModel = hiltViewModel()
) {

    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity


    DisposableEffect(key1 = viewModel) {
        viewModel.onStart(onFaceScanReady)
        onDispose { }
    }

    LaunchedEffect(key1 = state) {

        if (state.initFacetecData is Resource.Success) {
            if (BuildConfig.FACETEC_ENABLED) {
                FaceTecSDK.initializeInProductionMode(
                    context,
                    state.facetecData?.productionKeyText ?: "",
                    state.facetecData?.deviceKeyId ?: "",
                    state.facetecData?.biometryEncryptionPublicKey ?: "",
                    object : FaceTecSDK.InitializeCallback() {
                        override fun onCompletion(successful: Boolean) {
                            if (successful) {
                                viewModel.facetecSDKInitialized()
                            } else {
                                viewModel.failedToInitializeSDK()
                            }
                        }
                    }
                )
                FaceTecSDK.setCustomization(viewModel.facetecCustomizations())
            } else {
                viewModel.facetecSDKInitialized()
            }

            viewModel.resetFacetecInitDataResource()
        }

        if (state.startAuth is Resource.Success) {
            if (BuildConfig.FACETEC_ENABLED) {
                FaceTecSessionActivity.createAndLaunchSession(
                    context,
                    viewModel,
                    state.facetecData?.sessionToken ?: ""
                )
            } else {
                onFaceScanReady(
                    state.facetecData?.id ?: BiometryVerificationId(""),
                    FacetecBiometry(
                        "",
                        "",
                        ""
                    )
                )
            }

            viewModel.resetStartFacetecAuth()
        }

        if (state.userCancelled is Resource.Success) {
            onCancelled()
            viewModel.resetUserCanceled()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(color = Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        when {
            state.apiError -> {
                Text(
                    text = when (state.submitResultResponse) {
                        is Resource.Error -> state.submitResultResponse.exception?.message
                        else -> null
                    } ?: stringResource(R.string.error_occurred),
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                )
                Spacer(modifier = Modifier.height(24.dp))
                StandardButton(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    onClick = {
                        viewModel.retry()
                    }
                ) {
                    Text(
                        text = "Retry",
                        color = Color.White,
                        fontSize = 20.sp
                    )
                }
            }

            else -> LargeLoading(
                color = Color.Black,
                fullscreen = false
            )
        }
    }
}
