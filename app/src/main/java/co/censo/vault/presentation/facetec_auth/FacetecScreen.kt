package co.censo.vault.presentation.facetec_auth

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.navigation.NavController
import co.censo.vault.data.Resource
import co.censo.vault.presentation.home.Screen
import co.censo.vault.util.vaultLog
import com.facetec.sdk.FaceTecSDK
import com.facetec.sdk.FaceTecSessionActivity

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacetecAuthScreen(
    navController: NavController,
    viewModel: FacetecAuthViewModel = hiltViewModel()
) {

    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    LaunchedEffect(key1 = state) {

        if (state.initFacetecData is Resource.Success) {
            FaceTecConfig.initializeFaceTecSDKInDevelopmentMode(
                context,
                state.deviceKeyId,
                state.biometryEncryptionPublicKey,
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
            FaceTecSessionActivity.createAndLaunchSession(context, viewModel, state.sessionToken)
            viewModel.resetStartFacetecAuth()
        }

        if (state.submitResultResponse is Resource.Success) {
            navController.navigate(Screen.HomeRoute.route)
            viewModel.resetSubmitResult()
        }
    }

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier
                    .fillMaxWidth(),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Facetec Authorization")
                    }
                })
        },
        content = {
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

                    state.userResponse is Resource.Success -> {
                        Button(onClick = { viewModel.retrieveFacetecData() }) {
                            Text(text = "Start Facetec Auth")
                        }
                    }

                    state.submitResultResponse is Resource.Success -> {
                        Text(text = "Result successfully sent to backend")
                    }
                }
            }
        }
    )
}
