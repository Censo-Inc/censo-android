package co.censo.censo.presentation.enter_phrase

import Base58EncodedMasterPublicKey
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import co.censo.shared.data.Resource
import co.censo.shared.presentation.components.DisplayError
import co.censo.censo.R
import co.censo.censo.presentation.Screen
import co.censo.censo.presentation.components.SeedPhraseAdded
import co.censo.censo.presentation.components.YesNoDialog
import co.censo.censo.presentation.enter_phrase.components.AddPhraseLabelUI
import co.censo.censo.presentation.enter_phrase.components.ReviewSeedPhraseUI
import co.censo.censo.presentation.enter_phrase.components.indexToWordText
import co.censo.censo.presentation.enter_phrase.components.EditPhraseWordUI
import co.censo.censo.presentation.enter_phrase.components.PastePhraseUI
import co.censo.censo.presentation.enter_phrase.components.SelectSeedPhraseEntryType
import co.censo.censo.presentation.enter_phrase.components.ViewPhraseWordUI
import co.censo.shared.presentation.components.LargeLoading
import co.censo.shared.util.ClipboardHelper
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.sendError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterPhraseScreen(
    masterPublicKey: Base58EncodedMasterPublicKey,
    welcomeFlow: Boolean,
    navController: NavController,
    viewModel: EnterPhraseViewModel = hiltViewModel()
) {
    val context = LocalContext.current as FragmentActivity

    val state = viewModel.state

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {
            viewModel.finishPushNotificationDialog()
        }
    )

    val title = when (state.enterWordUIState) {
        EnterPhraseUIState.EDIT -> state.editedWordIndex.indexToWordText(context)
        EnterPhraseUIState.SELECT_ENTRY_TYPE,
        EnterPhraseUIState.PASTE_ENTRY,
        EnterPhraseUIState.SELECTED -> ""

        EnterPhraseUIState.REVIEW,
        EnterPhraseUIState.VIEW,
        EnterPhraseUIState.LABEL,
        EnterPhraseUIState.DONE -> stringResource(R.string.add_seed_phrase_title)
    }

    val iconPair =
        if (state.backArrowType == BackIconType.BACK) Icons.Filled.ArrowBack to R.string.back
        else Icons.Filled.Clear to R.string.exit

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart(
            welcomeFlow = welcomeFlow,
            masterPublicKey = masterPublicKey
        )
        onDispose {}
    }

    fun checkNotificationsPermissionDialog() {
        try {
            val notificationGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                )

            if (notificationGranted != PackageManager.PERMISSION_GRANTED) {
                val shownPermissionBefore =
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                val seenDialogBefore = viewModel.userHasSeenPushDialog()

                if (!shownPermissionBefore && !seenDialogBefore) {
                    viewModel.setUserSeenPushDialog(true)
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

        } catch (e: Exception) {
            e.sendError(CrashReportingUtil.PermissionDialog)
        }
    }

    LaunchedEffect(key1 = state) {
        if (state.phraseEntryComplete is Resource.Success) {
            if (welcomeFlow) {
                navController.navigate(
                    Screen.OwnerVaultScreen.route
                )
            } else {
                navController.popBackStack()
            }
            viewModel.resetPhraseEntryComplete()
        }

        if (state.showPushNotificationsDialog is Resource.Success) {
            checkNotificationsPermissionDialog()
        }

        if (state.exitFlow) {
            navController.navigate(Screen.EntranceRoute.route)
            viewModel.resetExitFlow()
        }
    }

    BackHandler(enabled = state.enterWordUIState == EnterPhraseUIState.SELECT_ENTRY_TYPE) {
        viewModel.onBackClicked()
    }

    Scaffold(topBar = {
        if (state.enterWordUIState != EnterPhraseUIState.SELECT_ENTRY_TYPE) {
            CenterAlignedTopAppBar(
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
                },
            )
        }
    }) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            when {
                state.loading -> LargeLoading(fullscreen = true)

                state.error -> {
                    if (state.submitResource is Resource.Error) {
                        DisplayError(
                            errorMessage = stringResource(R.string.failed_save_seed),
                            dismissAction = viewModel::resetSubmitResourceErrorState,
                            retryAction = viewModel::resetSubmitResourceErrorState
                        )
                    } else if (state.userResource is Resource.Error) {
                        DisplayError(
                            errorMessage = stringResource(R.string.error_occurred_trying_to_get_user_data_please_try_again),
                            dismissAction = viewModel::resetUserResourceAndRetryGetUserApiCall,
                            retryAction = viewModel::resetUserResourceAndRetryGetUserApiCall
                        )
                    }

                }

                else -> {
                    if (state.exitConfirmationDialog) {
                        YesNoDialog(
                            title = stringResource(R.string.exit_seed_phrase_entry),
                            message = stringResource(R.string.exit_seed_phrase_entry_message),
                            onDismiss = viewModel::hideExitConfirmationDialog,
                            onConfirm = viewModel::exitFlow
                        )
                    }

                    if (state.cancelInputSeedPhraseConfirmationDialog) {
                        YesNoDialog(
                            title = stringResource(R.string.exit_seed_phrase_entry),
                            message = stringResource(R.string.exit_seed_phrase_entry_message),
                            onDismiss = viewModel::hideCancelInputSeedPhraseConfirmationDialog,
                            onConfirm = {
                                viewModel.navigateToSeedPhraseType()
                                viewModel.hideCancelInputSeedPhraseConfirmationDialog()
                            }
                        )
                    }

                    when (state.enterWordUIState) {
                        EnterPhraseUIState.SELECT_ENTRY_TYPE -> {
                            SelectSeedPhraseEntryType(
                                welcomeFlow = state.welcomeFlow,
                                currentLanguage = state.currentLanguage,
                                onManualEntrySelected = { language -> viewModel.entrySelected(EntryType.MANUAL, language) },
                                onPasteEntrySelected = { viewModel.entrySelected(EntryType.PASTE) }
                            )
                        }

                        EnterPhraseUIState.PASTE_ENTRY -> {
                            PastePhraseUI {
                                viewModel.onPhrasePasted(
                                    ClipboardHelper.getClipboardContent(context) ?: ""
                                )
                                ClipboardHelper.clearClipboardContent(context)
                            }
                        }

                        EnterPhraseUIState.EDIT, EnterPhraseUIState.SELECTED -> {
                            EditPhraseWordUI(
                                phraseWord = state.editedWord,
                                enterWordUIState = state.enterWordUIState,
                                updateEditedWord = viewModel::updateEditedWord,
                                onWordSelected = viewModel::wordSelected,
                                wordSubmitted = viewModel::wordSubmitted,
                                language = state.currentLanguage
                            )
                        }

                        EnterPhraseUIState.VIEW -> {
                            ViewPhraseWordUI(
                                editedWordIndex = state.editedWordIndex,
                                phraseWord = if (state.editedWordIndex < 0
                                    || state.editedWordIndex >= state.enteredWords.size
                                ) {
                                    ""
                                } else {
                                    state.enteredWords[state.editedWordIndex]
                                },
                                editExistingWord = viewModel::editExistingWord,
                                decrementEditIndex = viewModel::decrementEditIndex,
                                incrementEditIndex = viewModel::incrementEditIndex,
                                enterNextWord = viewModel::enterNextWord,
                                submitFullPhrase = viewModel::submitFullPhrase,
                                deleteExistingWord = viewModel::deleteExistingWord,
                            )
                        }

                        EnterPhraseUIState.REVIEW -> {
                            ReviewSeedPhraseUI(
                                invalidReason = state.phraseInvalidReason,
                                phraseWords = state.enteredWords,
                                saveSeedPhrase = viewModel::moveToLabel,
                                editSeedPhrase = viewModel::editEntirePhrase

                            )
                        }

                        EnterPhraseUIState.LABEL -> {
                            AddPhraseLabelUI(
                                label = state.label,
                                enabled = state.labelValid,
                                labelIsTooLong = state.labelIsTooLong,
                                onLabelChanged = viewModel::updateLabel,
                                onSavePhrase = viewModel::saveSeedPhrase
                            )
                        }

                        EnterPhraseUIState.DONE -> {
                            SeedPhraseAdded(
                                isSavingFirstSeedPhrase = state.isSavingFirstSeedPhrase,
                                onClick = viewModel::finishPhraseEntry
                            )
                        }
                    }
                }
            }
        }
    }
}