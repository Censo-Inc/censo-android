package co.censo.censo.presentation.access_seed_phrases

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.components.DisplayError
import co.censo.censo.R
import co.censo.censo.presentation.access_seed_phrases.components.AccessPhrasesTopBar
import co.censo.censo.presentation.access_seed_phrases.components.ReadyToAccessPhrase
import co.censo.censo.presentation.access_seed_phrases.components.SelectPhraseUI
import co.censo.censo.presentation.access_seed_phrases.components.Bip39Review
import co.censo.censo.presentation.components.ImageReview
import co.censo.censo.presentation.components.YesNoDialog
import co.censo.censo.presentation.facetec_auth.FacetecAuth
import co.censo.censo.util.launchSingleTopIfNavigatingToHomeScreen
import co.censo.shared.data.model.SeedPhraseData
import co.censo.shared.data.model.getImageBitmap
import co.censo.shared.util.popCurrentDestinationFromBackStack
import co.censo.shared.presentation.components.LargeLoading
import kotlin.time.Duration

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
            Lifecycle.Event.ON_START -> viewModel.onStart()
            Lifecycle.Event.ON_RESUME -> viewModel.onResume()
            Lifecycle.Event.ON_PAUSE -> viewModel.onPause()
            else -> Unit
        }
    }

    LaunchedEffect(key1 = state) {
        if (state.navigationResource is Resource.Success) {
            state.navigationResource.data.let { navigationData ->
                navController.navigate(navigationData.route) {
                    launchSingleTopIfNavigatingToHomeScreen(navigationData.route)
                    if (navigationData.popSelfFromBackStack) {
                        popCurrentDestinationFromBackStack(navController)
                    }
                }
                viewModel.delayedResetNavigationResource()
            }
        }
    }

    BackHandler(enabled = true) {
        if (state.accessPhrasesUIState == AccessPhrasesUIState.SelectPhrase) {
            viewModel.showCancelConfirmationDialog()
        } else {
            viewModel.onBackClicked()
        }
    }


    Scaffold(
        topBar = {
            AccessPhrasesTopBar(
                accessPhrasesUIState = state.accessPhrasesUIState,
                phraseLabel = state.selectedPhrase?.label,
                onNavClicked = {
                    if (state.accessPhrasesUIState == AccessPhrasesUIState.SelectPhrase) {
                        viewModel.showCancelConfirmationDialog()
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
                state.loading -> LargeLoading(
                    fullscreenBackgroundColor = Color.White,
                    fullscreen = true
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

                        state.cancelAccessResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.cancelAccessResource.getErrorMessage(context),
                                dismissAction = null,
                            ) { viewModel.cancelAccess() }
                        }
                    }
                }

                else -> {

                    if (state.showCancelConfirmationDialog) {
                        YesNoDialog(
                            title = stringResource(R.string.exit_accessing_phrases),
                            message = stringResource(
                                R.string.exit_accessing_phrases_message,
                                stringResource(if (state.hasExternalApprovers) {
                                    R.string.request_approval_lower
                                } else {
                                    R.string.wait_for_timelock
                                })
                            ),
                            onDismiss = viewModel::hideCloseConfirmationDialog,
                            onConfirm = viewModel::cancelAccess
                        )
                    }

                    when (state.accessPhrasesUIState) {
                        AccessPhrasesUIState.SelectPhrase -> {
                            SelectPhraseUI(
                                seedPhrases = (state.ownerState.success()?.data as? OwnerState.Ready)?.vault?.seedPhrases ?: listOf(),
                                viewedIds = state.viewedPhraseIds,
                                onPhraseSelected = viewModel::onPhraseSelected,
                                onFinish = viewModel::showCancelConfirmationDialog
                            )
                        }

                        AccessPhrasesUIState.ReadyToStart -> {
                            ReadyToAccessPhrase(getStarted = viewModel::startFacetec)
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
                            state.recoveredPhrases.success()?.data?.first()?.let {
                                ViewPhrase(
                                    seedPhrase = it.seedPhrase,
                                    timeRemaining = state.timeRemaining,
                                    onReset = viewModel::reset
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ViewPhrase(
    seedPhrase: SeedPhraseData,
    timeRemaining: Duration,
    onReset: () -> Unit,
) {
    when (seedPhrase) {
        is SeedPhraseData.Image -> {
            when (val recoveredImage = seedPhrase.getImageBitmap()) {
                null -> {
                    DisplayError(
                        errorMessage = stringResource(R.string.unable_to_render_image_for_review),
                        dismissAction = onReset,
                        retryAction = onReset
                    )
                }

                else -> {
                    ImageReview(
                        imageBitmap = recoveredImage,
                        onDoneViewing = onReset,
                        timeLeft = timeRemaining,
                        isAccessReview = true
                    )
                }
            }
        }

        is SeedPhraseData.Bip39 -> {
            Bip39Review(
                phraseWords = seedPhrase.words,
                onDone = onReset,
                timeLeft = timeRemaining
            )
        }
    }
}
