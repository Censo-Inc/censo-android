package co.censo.vault.presentation.access_seed_phrases

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.components.DisplayError
import co.censo.vault.R
import co.censo.vault.presentation.VaultColors
import co.censo.vault.presentation.access_seed_phrases.components.AccessPhrasesTopBar
import co.censo.vault.presentation.access_seed_phrases.components.ReadyToAccessPhrase
import co.censo.vault.presentation.access_seed_phrases.components.SelectPhraseUI
import co.censo.vault.presentation.access_seed_phrases.components.ViewAccessPhraseUI
import co.censo.vault.presentation.facetec_auth.FacetecAuth

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AccessSeedPhrasesScreen(
    navController: NavController,
    viewModel: AccessSeedPhrasesViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME
            -> {
                viewModel.onStart()
            }

            Lifecycle.Event.ON_PAUSE -> {
                viewModel.onStop()
            }

            else -> Unit
        }
    }

    LaunchedEffect(key1 = state) {
        if (state.navigationResource is Resource.Success) {
            state.navigationResource.data?.let {
                navController.navigate(it)
                viewModel.resetNavigationResource()
            }
        }
    }


    Scaffold(
        topBar = {
            AccessPhrasesTopBar(
                accessPhrasesUIState = state.accessPhrasesUIState,
                phraseLabel = state.selectedPhrase?.label,
                onNavClicked = {
                    if (state.accessPhrasesUIState == AccessPhrasesUIState.SelectPhrase) {
                        //Exit entire flow
                        navController.popBackStack()
                    } else {
                        viewModel.onBackClicked()
                    }
                }
            )
        }
    ) { contentPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {

            when {
                state.loading ->
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(72.dp)
                            .align(Alignment.Center),
                        strokeWidth = 8.dp,
                        color = Color.White
                    )

                state.asyncError -> {
                    when {
                        state.ownerState is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.ownerState.getErrorMessage(context),
                                dismissAction = null,
                            ) { viewModel.retrieveOwnerState() }
                        }

                        state.retrieveShardsResponse is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.retrieveShardsResponse.getErrorMessage(context),
                                dismissAction = null,
                            ) { viewModel.reset() }
                        }

                        state.recoveredPhrases is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.recoveredPhrases.getErrorMessage(context),
                                dismissAction = null,
                            ) { viewModel.reset() }
                        }
                    }
                }

                else -> {

                    when (state.accessPhrasesUIState) {
                        AccessPhrasesUIState.SelectPhrase -> {
                            (state.ownerState.data as? OwnerState.Ready)?.vault?.secrets?.let { vaultSecrets ->
                                SelectPhraseUI(
                                    vaultSecrets = vaultSecrets,
                                    onPhraseSelected = viewModel::onPhraseSelected
                                )
                            } ?: DisplayError(
                                errorMessage = "Missing phrase data",
                                dismissAction = viewModel::retrieveOwnerState,
                                retryAction = viewModel::retrieveOwnerState
                            )
                        }

                        AccessPhrasesUIState.ReadyToStart -> {
                            ReadyToAccessPhrase(
                                phraseLabel = state.selectedPhrase?.label
                                    ?: stringResource(R.string.phrase)
                            ) {
                                viewModel.startFacetec()
                            }
                        }

                        AccessPhrasesUIState.Facetec -> {
                            FacetecAuth(
                                onFaceScanReady = { verificationId, biometry ->
                                    viewModel.onFaceScanReady(
                                        verificationId,
                                        biometry
                                    )
                                }
                            )
                        }

                        AccessPhrasesUIState.ViewPhrase -> {
                            state.recoveredPhrases.data?.first()?.let {
                                ViewAccessPhraseUI(
                                    wordIndex = state.selectedIndex,
                                    phraseWord = state.selectedWord,
                                    decrementIndex = viewModel::decrementIndex,
                                    incrementIndex = viewModel::incrementIndex,
                                    onDone = viewModel::reset,
                                    timeLeft = state.timeRemaining
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
