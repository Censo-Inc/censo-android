package co.censo.vault.presentation.facetec_auth

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import co.censo.shared.data.Resource
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.FacetecBiometry
import com.facetec.sdk.FaceTecSDK
import com.facetec.sdk.FaceTecSessionActivity

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun FacetecAuth(
    onFaceScanReady: suspend (BiometryVerificationId, FacetecBiometry) -> Resource<BiometryScanResultBlob>,
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
            viewModel.resetFacetecInitDataResource()
        }

        if (state.startAuth is Resource.Success) {
            FaceTecSessionActivity.createAndLaunchSession(context, viewModel, state.facetecData?.sessionToken ?: "")
            viewModel.resetStartFacetecAuth()
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
            state.loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(72.dp),
                    strokeWidth = 8.dp,
                    color = Color.Red
                )
            }

            state.apiError -> {
                Text(text = "Error Occurred")
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.retry()
                    }
                ) {
                    Text(text = "Retry")
                }
            }

            state.submitResultResponse is Resource.Success -> {
                Text(text = "Authorized")
            }
        }
    }
}
