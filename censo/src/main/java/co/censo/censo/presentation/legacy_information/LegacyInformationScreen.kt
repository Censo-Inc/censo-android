package co.censo.censo.presentation.legacy_information

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.censo.R
import co.censo.censo.presentation.legacy_information.components.ApproversContactInfoUI
import co.censo.censo.presentation.legacy_information.components.LegacyInfoUI
import co.censo.censo.presentation.components.SeedPhraseNotesUI
import co.censo.censo.presentation.components.SeedPhraseNotesUIEntryPoint
import co.censo.censo.presentation.legacy_information.components.SelectInfoTypeUI
import co.censo.censo.presentation.legacy_information.components.SelectSeedPhraseUI
import co.censo.shared.data.Resource
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.presentation.components.LargeLoading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegacyInformationScreen(
    navController: NavController,
    viewModel: LegacyInformationViewModel = hiltViewModel()
) {

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> viewModel.onStart()
            else -> Unit
        }
    }

    val state = viewModel.state
    val context = LocalContext.current as FragmentActivity

    LaunchedEffect(key1 = state) {
        if (state.navigationResource is Resource.Success) {
            state.navigationResource.data.let { navigationData ->
                navController.navigate(navigationData.route)
                viewModel.resetNavigationResource()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = viewModel::onTopBarBackClicked
                    ) {
                        Icon(
                            imageVector = if (state.uiState is LegacyInformationState.UIState.Information) Icons.Filled.Close else Icons.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back),
                        )
                    }
                },
                title = {
                    Text(
                        text = when (val uiState = state.uiState) {
                            is LegacyInformationState.UIState.Information,
                            is LegacyInformationState.UIState.InformationType -> stringResource(R.string.legacy_title_information_type)
                            is LegacyInformationState.UIState.ApproverInformation -> stringResource(R.string.legacy_title_approver_information)
                            is LegacyInformationState.UIState.SelectSeedPhrase -> stringResource(R.string.legacy_title_seed_phrase_information)
                            is LegacyInformationState.UIState.SeedPhraseInformation -> stringResource(
                                R.string.legacy_title_seed_phrase_id, uiState.seedPhrase.label
                            )
                        },
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.W400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(color = Color.White)
        ) {

            when {
                state.loading -> LargeLoading(fullscreen = true)

                state.asyncError -> {
                    when {
                        state.decryptContactInfoResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.decryptContactInfoResource.getErrorMessage(context),
                                dismissAction = viewModel::resetDecryptContactInfoResource,
                                retryAction = null
                            )
                        }
                        state.updateContactInfoResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.updateContactInfoResource.getErrorMessage(context),
                                dismissAction = viewModel::resetUpdateContactInfoResource,
                                retryAction = null
                            )
                        }
                        state.decryptSeedPhraseInfoResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.decryptSeedPhraseInfoResource.getErrorMessage(context),
                                dismissAction = viewModel::resetDecryptSeedPhraseInfoResource,
                                retryAction = null
                            )
                        }
                        state.updateSeedPhraseInfoResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.updateSeedPhraseInfoResource.getErrorMessage(context),
                                dismissAction = viewModel::resetUpdateSeedPhraseInfoResource,
                                retryAction = null
                            )
                        }
                        state.retrieveKeyResource is Resource.Error -> {
                            DisplayError(
                                errorMessage = state.retrieveKeyResource.getErrorMessage(context),
                                dismissAction = null,
                                retryAction = { viewModel.retrieveKeyFromTheCloud() }
                            )
                        }
                    }
                }

                else -> {
                    when (val uiState = state.uiState) {
                        LegacyInformationState.UIState.Information -> {
                            LegacyInfoUI(
                                onContinue = viewModel::moveUIToInformationType
                            )
                        }

                        LegacyInformationState.UIState.InformationType -> {
                            SelectInfoTypeUI(
                                onApproverInfo = viewModel::moveUIToApproverInfo,
                                onSeedPhraseInfo = viewModel::moveUIToSelectSeedPhrase
                            )
                        }

                        is LegacyInformationState.UIState.ApproverInformation -> {
                            ApproversContactInfoUI(
                                contactInfo = uiState.contactInfo,
                                onSave = viewModel::saveApproverContactInformation
                            )
                        }

                        is LegacyInformationState.UIState.SelectSeedPhrase -> {
                            SelectSeedPhraseUI(
                                seedPhrases = uiState.seedPhrases,
                                onSeedPhraseSelected = viewModel::moveUIToSeedPhraseInformation
                            )
                        }

                        is LegacyInformationState.UIState.SeedPhraseInformation -> {
                            SeedPhraseNotesUI(
                                notes = uiState.seedPhrase.notes,
                                onContinue = { notes -> viewModel.saveSeedPhraseInformation(uiState.seedPhrase.guid, notes) },
                                entryPoint = SeedPhraseNotesUIEntryPoint.LegacyInformation,
                            )
                        }
                    }
                }
            }
        }
    }
}
