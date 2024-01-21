package co.censo.censo.presentation.enter_phrase

import Base58EncodedMasterPublicKey
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import co.censo.censo.MainActivity
import co.censo.shared.data.Resource
import co.censo.shared.presentation.components.DisplayError
import co.censo.censo.R
import co.censo.censo.presentation.Screen
import co.censo.censo.presentation.components.CameraView
import co.censo.censo.presentation.components.SeedPhraseAdded
import co.censo.censo.presentation.components.SimpleAlertDialog
import co.censo.censo.presentation.components.YesNoDialog
import co.censo.censo.presentation.enter_phrase.components.AddPhraseLabelUI
import co.censo.censo.presentation.enter_phrase.components.ReviewSeedPhraseUI
import co.censo.censo.presentation.enter_phrase.components.indexToWordText
import co.censo.censo.presentation.enter_phrase.components.EditPhraseWordUI
import co.censo.censo.presentation.enter_phrase.components.GeneratePhraseUI
import co.censo.censo.presentation.enter_phrase.components.PastePhraseUI
import co.censo.censo.presentation.enter_phrase.components.SelectSeedPhraseEntryType
import co.censo.censo.presentation.enter_phrase.components.ViewPhraseWordUI
import co.censo.censo.presentation.paywall.PaywallViewModel
import co.censo.censo.presentation.push_notification.PushNotificationScreen
import co.censo.shared.data.model.OwnerState
import co.censo.shared.presentation.OnLifecycleEvent
import co.censo.shared.util.popCurrentDestinationFromBackStack
import co.censo.shared.presentation.SharedColors
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.presentation.cloud_storage.CloudStorageHandler
import co.censo.shared.presentation.components.ConfirmationDialog
import co.censo.shared.presentation.components.LargeLoading
import co.censo.shared.util.ClipboardHelper
import co.censo.shared.util.errorMessage
import co.censo.shared.util.errorTitle
import co.censo.shared.util.projectLog
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun EnterPhraseScreen(
    masterPublicKey: Base58EncodedMasterPublicKey,
    welcomeFlow: Boolean,
    navController: NavController,
    importingPhrase: Boolean = false,
    encryptedPhrase: String,
    paywallViewModel: PaywallViewModel,
    viewModel: EnterPhraseViewModel = hiltViewModel()
) {
    val context = LocalContext.current as FragmentActivity
    val keyboardController = LocalSoftwareKeyboardController.current

    val state = viewModel.state

    val cameraExecutor = Executors.newSingleThreadExecutor()

    val title = when (state.enterWordUIState) {
        EnterPhraseUIState.EDIT -> state.editedWordIndex.indexToWordText(context)
        EnterPhraseUIState.SELECT_ENTRY_TYPE,
        EnterPhraseUIState.SELECT_ENTRY_TYPE_OWN,
        EnterPhraseUIState.CAPTURE_IMAGE,
        EnterPhraseUIState.PASTE_ENTRY,
        EnterPhraseUIState.SELECTED,
        EnterPhraseUIState.NOTIFICATIONS -> ""

        EnterPhraseUIState.REVIEW_WORDS,
        EnterPhraseUIState.REVIEW_IMAGE,
        EnterPhraseUIState.VIEW,
        EnterPhraseUIState.LABEL,
        EnterPhraseUIState.GENERATE,
        EnterPhraseUIState.DONE -> stringResource(R.string.add_seed_phrase_title)
    }

    val iconPair =
        if (state.backArrowType == BackIconType.BACK) Icons.Filled.ArrowBack to R.string.back
        else Icons.Filled.Clear to R.string.exit

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart(
            importingPhrase = importingPhrase,
            welcomeFlow = welcomeFlow,
            masterPublicKey = masterPublicKey,
            encryptedPhrase = encryptedPhrase
        )
        onDispose {}
    }

    LaunchedEffect(key1 = state) {
        if (state.phraseEntryComplete is Resource.Success) {
            if (welcomeFlow) {
                navController.navigate(
                    Screen.OwnerVaultScreen.route
                ) {
                    launchSingleTop = true
                    popCurrentDestinationFromBackStack(navController)
                }
            } else {
                navController.popBackStack()
            }
            viewModel.resetPhraseEntryComplete()
        }

        if (state.exitFlow) {
            //If welcomeFlow then nav back to the Entrance route, else nav back to Owner Vault route
            val route =
                if (welcomeFlow) Screen.EntranceRoute.route else Screen.OwnerVaultScreen.route

            navController.navigate(route) {
                launchSingleTop = true
                popCurrentDestinationFromBackStack(navController)
            }

            viewModel.resetExitFlow()
        }

        if (state.triggerPaywallUI is Resource.Success) {
            paywallViewModel.setPaywallVisibility(
                ignoreSubscriptionRequired = true,
                onSuccessfulPurchase = {
                    viewModel.subscriptionCompleted()
                },
                onCancelPurchase = {
                    paywallViewModel.resetVisibility()
                }
            )
            viewModel.resetPaywallTrigger()
        }
    }

    //enabled = true, keeps the back OS button behavior in line with the back nav icon functionality
    BackHandler(enabled = true) {
        viewModel.onBackClicked()
    }

    Scaffold(topBar = {
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
                        color = SharedColors.MainColorText
                    )
                }
            },
        )
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
                            retryAction = viewModel::saveSeedPhrase
                        )
                    } else if (state.userResource is Resource.Error) {
                        DisplayError(
                            errorMessage = stringResource(R.string.error_occurred_trying_to_get_user_data_please_try_again),
                            dismissAction = viewModel::resetUserResourceAndRetryGetUserApiCall,
                            retryAction = viewModel::resetUserResourceAndRetryGetUserApiCall
                        )
                    } else if (state.deleteUserResource is Resource.Error) {
                        DisplayError(
                            errorMessage = stringResource(R.string.reset_user_data_error),
                            dismissAction = viewModel::onCancelResetUser,
                            retryAction = viewModel::deleteUser
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

                    if (state.showInvalidPhraseDialog is Resource.Success) {
                        val invalidPhraseTitle = state.showInvalidPhraseDialog.data.errorTitle()
                        val invalidPhraseMessage = state.showInvalidPhraseDialog.data.errorMessage()

                        SimpleAlertDialog(
                            title = invalidPhraseTitle,
                            message = invalidPhraseMessage,
                            onDismiss = viewModel::removeInvalidPhraseDialog,
                        )
                    }

                    when (state.enterWordUIState) {
                        EnterPhraseUIState.SELECT_ENTRY_TYPE, EnterPhraseUIState.SELECT_ENTRY_TYPE_OWN -> {
                            val savedPhrases =
                                ((state.userResource.success()?.data?.ownerState) as? OwnerState.Ready)?.vault?.seedPhrases?.count() ?: 0

                            SelectSeedPhraseEntryType(
                                welcomeFlow = state.welcomeFlow,
                                savedPhrases = savedPhrases,
                                currentLanguage = state.currentLanguage,
                                onManualEntrySelected = { language ->
                                    viewModel.entrySelected(
                                        EntryType.MANUAL,
                                        language
                                    )
                                },
                                onPasteEntrySelected = { viewModel.entrySelected(EntryType.PASTE) },
                                onGenerateEntrySelected = {  language -> viewModel.entrySelected(EntryType.GENERATE, language) },
                                userHasOwnPhrase = state.enterWordUIState == EnterPhraseUIState.SELECT_ENTRY_TYPE_OWN,
                                onUserHasOwnPhrase = { viewModel.setUserHasOwnPhrase() },
                                onPictureEntrySelected = { viewModel.entrySelected(EntryType.IMAGE) }
                            )
                            if (state.triggerDeleteUserDialog is Resource.Success) {
                                ConfirmationDialog(
                                    title = stringResource(id = R.string.exit_setup),
                                    message = stringResource(R.string.exit_setup_details),
                                    onCancel = viewModel::onCancelResetUser,
                                    onDelete = viewModel::deleteUser,
                                )
                            }
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

                        EnterPhraseUIState.GENERATE -> {
                            GeneratePhraseUI(
                                selectedWordCount = state.desiredGeneratedPhraseLength,
                                onWordCountSelected = viewModel::onDesiredGeneratedPhraseLengthSelected,
                                onGenerate = viewModel::generatePhrase
                            )
                        }

                        EnterPhraseUIState.CAPTURE_IMAGE -> {}

                        EnterPhraseUIState.REVIEW_IMAGE -> {
                            //TODO: ImageReviewUI
                            // Refine this
                            // Save image
                            // Cancel image (and take another)
                            Box(modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Green)) {
                                state.imageBitmap?.let {
                                    Image(bitmap = it.asImageBitmap(), contentDescription = null)
                                }
                            }
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

                        EnterPhraseUIState.REVIEW_WORDS -> {
                            ReviewSeedPhraseUI(
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
                                onSavePhrase = {
                                    keyboardController?.hide()
                                    viewModel.saveSeedPhrase()
                                }
                            )
                        }

                        EnterPhraseUIState.DONE -> {
                            SeedPhraseAdded(
                                onClick = viewModel::finishPhraseEntry
                            )
                        }

                        EnterPhraseUIState.NOTIFICATIONS -> {
                            PushNotificationScreen(onFinished = viewModel::finishPushNotificationScreen)
                        }
                    }

                }
            }
        }
    }

    if (state.cloudStorageAction.triggerAction && state.cloudStorageAction.action == CloudStorageActions.DOWNLOAD) {
        val participantId = state.ownerApproverParticipantId
        if (participantId != null) {
            CloudStorageHandler(
                actionToPerform = state.cloudStorageAction.action,
                participantId = participantId,
                encryptedPrivateKey = null,
                onActionSuccess = { viewModel.onKeyDownloadSuccess(it) },
                onActionFailed = { viewModel.onKeyDownloadFailed(it) }
            )
        } else {
            viewModel.onKeyDownloadFailed(
                exception = Exception("Unable to load key data, missing participant id")
            )
        }
    }
}