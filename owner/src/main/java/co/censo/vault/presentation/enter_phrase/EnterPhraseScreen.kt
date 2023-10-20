package co.censo.vault.presentation.enter_phrase

import Base58EncodedMasterPublicKey
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.shared.SharedScreen
import co.censo.shared.data.Resource
import co.censo.shared.presentation.components.DisplayError
import co.censo.shared.R as SharedR
import co.censo.vault.R
import co.censo.vault.presentation.Screen
import co.censo.vault.presentation.VaultColors
import co.censo.vault.presentation.enter_phrase.components.AddPhraseNicknameUI
import co.censo.vault.presentation.enter_phrase.components.ReviewSeedPhraseUI
import co.censo.vault.presentation.enter_phrase.components.indexToWordText
import co.censo.vault.presentation.enter_phrase.components.EditPhraseWordUI
import co.censo.vault.presentation.enter_phrase.components.SelectSeedPhraseEntryType
import co.censo.vault.presentation.enter_phrase.components.ViewPhraseWordUI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterPhraseScreen(
    masterPublicKey: Base58EncodedMasterPublicKey,
    welcomeFlow: Boolean,
    navController: NavController,
    viewModel: EnterPhraseViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state = viewModel.state

    val title = when (state.enterWordUIState) {
        EnterPhraseUIState.EDIT -> state.editedWordIndex.indexToWordText(context)
        EnterPhraseUIState.SELECT_ENTRY_TYPE,
        EnterPhraseUIState.NICKNAME,
        EnterPhraseUIState.SELECTED,
        EnterPhraseUIState.VIEW,
        EnterPhraseUIState.REVIEW -> ""
    }

    val iconPair =
        if (state.backArrowType == BackIconType.BACK) Icons.Filled.ArrowBack to R.string.back
        else Icons.Filled.Clear to R.string.exit

    DisposableEffect(key1 = state) {
        viewModel.onStart(
            welcomeFlow = welcomeFlow,
            masterPublicKey = masterPublicKey
        )
        onDispose {}
    }

    LaunchedEffect(key1 = state) {
        if (state.phraseEntryComplete is Resource.Success) {
            //TODO: Add add approvers flow if welcome flow is enabled
            navController.navigate(Screen.PlanSetupRoute.buildNavRoute(
                welcomeFlow = welcomeFlow,
                seedPhraseNickname = state.nickName
            ))
            viewModel.resetPhraseEntryComplete()
        }

        if (state.exitFlow) {
            navController.navigate(SharedScreen.EntranceRoute.route)
            viewModel.resetExitFlow()
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            colors = TopAppBarDefaults.smallTopAppBarColors(
                containerColor = VaultColors.NavbarColor
            ),
            navigationIcon = {
                IconButton(onClick = {
                    viewModel.onBackClicked()
                }) {
                    Icon(
                        imageVector = iconPair.first,
                        contentDescription = stringResource(id = iconPair.second),
                    )
                }
            },
            title = {
                if (title.isNotEmpty()) {
                    Text(
                        text = title,
                        color = Color.Black
                    )
                }
            }, actions = {
                IconButton(onClick = {
                    Toast.makeText(context, "Show FAQ Web View", Toast.LENGTH_LONG).show()
                }) {
                    Icon(
                        painterResource(id = SharedR.drawable.question),
                        contentDescription = "learn more"
                    )
                }
            })
    }) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            when {

                state.loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(72.dp),
                        strokeWidth = 5.dp
                    )
                }

                state.error -> {
                    DisplayError(
                        errorMessage = stringResource(R.string.failed_save_seed),
                        dismissAction = viewModel::setViewPhrase,
                        retryAction = viewModel::setViewPhrase
                    )
                }

                else -> {
                    when (state.enterWordUIState) {
                        EnterPhraseUIState.SELECT_ENTRY_TYPE -> {
                            SelectSeedPhraseEntryType(
                                welcomeFlow = state.welcomeFlow,
                                onManualEntrySelected = { viewModel.entrySelected(EntryType.MANUAL) },
                                onPasteEntrySelected = { viewModel.entrySelected(EntryType.PASTE) }
                            )
                        }

                        EnterPhraseUIState.EDIT, EnterPhraseUIState.SELECTED -> {
                            EditPhraseWordUI(
                                phraseWord = state.editedWord,
                                enterWordUIState = state.enterWordUIState,
                                updateEditedWord = viewModel::updateEditedWord,
                                onWordSelected = viewModel::wordSelected,
                                wordSubmitted = viewModel::wordSubmitted
                            )
                        }

                        EnterPhraseUIState.VIEW -> {
                            ViewPhraseWordUI(
                                editedWordIndex = state.editedWordIndex,
                                phraseWord = state.enteredWords[state.editedWordIndex],
                                editExistingWord = viewModel::editExistingWord,
                                decrementEditIndex = viewModel::decrementEditIndex,
                                incrementEditIndex = viewModel::incrementEditIndex,
                                enterNextWord = viewModel::enterNextWord,
                                submitFullPhrase = viewModel::submitFullPhrase
                            )
                        }

                        EnterPhraseUIState.REVIEW -> {
                            ReviewSeedPhraseUI(
                                valid = state.validPhrase,
                                phraseWords = state.enteredWords,
                                saveSeedPhrase = viewModel::moveToNickname,
                                editSeedPhrase = viewModel::editEntirePhrase

                            )
                        }

                        EnterPhraseUIState.NICKNAME -> {
                            AddPhraseNicknameUI(
                                nickname = state.nickName,
                                enabled = state.validName,
                                onNicknameChanged = viewModel::updateNickname,
                                onSavePhrase = viewModel::saveSeedPhrase
                            )
                        }
                    }
                }
            }
        }
    }
}