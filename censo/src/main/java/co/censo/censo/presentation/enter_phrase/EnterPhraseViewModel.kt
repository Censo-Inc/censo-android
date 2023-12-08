package co.censo.censo.presentation.enter_phrase

import Base58EncodedMasterPublicKey
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.networking.PushBody
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.data.repository.PushRepository
import co.censo.shared.data.repository.PushRepositoryImpl
import co.censo.shared.util.BIP39
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.sendError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.lang.Integer.max
import java.lang.Integer.min
import javax.inject.Inject

@HiltViewModel
class EnterPhraseViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val ownerStateFlow: MutableStateFlow<Resource<OwnerState>>,
    private val pushRepository: PushRepository,
    ) : ViewModel() {

    var state by mutableStateOf(EnterPhraseState())
        private set

    fun onStart(
        welcomeFlow: Boolean,
        masterPublicKey: Base58EncodedMasterPublicKey
    ) {
        state = state.copy(
            welcomeFlow = welcomeFlow,
            masterPublicKey = masterPublicKey
        )

        retrieveOwnerState()
    }

    //Only retrieving owner state to determine if this is the first seed phrase they are saving
    fun retrieveOwnerState() {
        state = state.copy(userResource = Resource.Loading())
        viewModelScope.launch {
            val response = ownerRepository.retrieveUser()

            state = state.copy(userResource = response)

            if (response is Resource.Success) {
                val ownerState = response.data!!.ownerState
                if (ownerState is OwnerState.Ready) {
                    state = state.copy(isSavingFirstSeedPhrase = ownerState.vault.seedPhrases.isEmpty())
                }
            }
        }
    }

    fun updateEditedWord(updatedWord: String) {
        state = state.copy(editedWord = updatedWord.lowercase().trim())
    }

    fun wordSelected(updatedWord: String) {
        state = state.copy(
            editedWord = updatedWord,
            enterWordUIState = EnterPhraseUIState.SELECTED
        )
    }

    fun wordSubmitted() {
        val phrase = state.enteredWords.toMutableList()

        if (state.editedWordIndex >= state.enteredWords.size) {
            phrase.add(state.editedWord)
        } else {
            phrase[state.editedWordIndex] = state.editedWord
        }

        state = state.copy(
            enterWordUIState = EnterPhraseUIState.VIEW,
            enteredWords = phrase
        )
    }

    fun submitFullPhrase() {
        state = state.copy(
            phraseInvalidReason = BIP39.validateSeedPhrase(state.enteredWords),
            enterWordUIState = EnterPhraseUIState.REVIEW
        )
    }

    fun enterNextWord() {
        state = state.copy(
            editedWordIndex = state.editedWordIndex + 1,
            editedWord = "",
            enterWordUIState = EnterPhraseUIState.EDIT
        )
    }

    fun incrementEditIndex(): Boolean {
        val currentIndex = state.editedWordIndex

        return if (currentIndex != state.enteredWords.size - 1
            && state.enteredWords.isNotEmpty()
            && currentIndex <= state.enteredWords.size - 1
        ) {
            state =
                state.copy(
                    editedWordIndex = state.editedWordIndex + 1,
                    editedWord = state.enteredWords[state.editedWordIndex + 1]
                )
            true
        } else {
            false
        }
    }

    fun decrementEditIndex(): Boolean {
        val currentIndex = state.editedWordIndex

        return if (currentIndex != 0) {
            state =
                state.copy(
                    editedWordIndex = state.editedWordIndex - 1,
                    editedWord = state.enteredWords[state.editedWordIndex - 1]
                )
            true
        } else {
            false
        }
    }

    fun editExistingWord() {
        state = state.copy(enterWordUIState = EnterPhraseUIState.EDIT)
    }

    fun deleteExistingWord() {
        val newEnteredWords = state.enteredWords.filterIndexed { ix, _ -> ix != state.editedWordIndex }
        state = if (newEnteredWords.isEmpty()) {
            state.copy(
                enteredWords = emptyList(),
                editedWordIndex = 0,
                enterWordUIState = EnterPhraseUIState.EDIT,
                editedWord = ""
            )
        } else {
            state.copy(
                enteredWords = newEnteredWords,
                editedWordIndex = max(0, min(state.editedWordIndex, newEnteredWords.size - 1))
            )
        }
    }

    fun moveToLabel() {
        viewModelScope.launch {
            if (state.masterPublicKey == null) {
                state = state.copy(
                    submitResource = Resource.Error(exception = Exception("Missing public key"))
                )
                return@launch
            }

            runCatching {
                // encrypt seed phrase and drop single words
                val encryptedSeedPhrase = ownerRepository.encryptSeedPhrase(
                    state.masterPublicKey!!,
                    state.enteredWords
                )

                state = state.copy(
                    enterWordUIState = EnterPhraseUIState.LABEL,
                    encryptedSeedPhrase = encryptedSeedPhrase,
                    enteredWords = listOf()
                )

            }.onFailure { throwable ->
                state = state.copy(
                    submitResource = Resource.Error(exception = Exception("Unable to encrypt seed phrase"))
                )
            }
        }
    }

    fun saveSeedPhrase() {
        state = state.copy(submitResource = Resource.Loading())

        viewModelScope.launch {
            val response = ownerRepository.storeSeedPhrase(
                state.label.trim(),
                state.encryptedSeedPhrase!!
            )

            state = state.copy(
                submitResource = response.map { },
                enterWordUIState = EnterPhraseUIState.DONE
            )

            if (response is Resource.Success) {
                ownerStateFlow.tryEmit(response.map { it.ownerState })
            }
        }
    }

    fun editEntirePhrase() {
        state = if (state.enteredWords.isEmpty()) {
            state.copy(enterWordUIState = EnterPhraseUIState.SELECT_ENTRY_TYPE)
        } else {
            state.copy(
                editedWord = state.enteredWords[0],
                editedWordIndex = 0,
                enterWordUIState = EnterPhraseUIState.VIEW
            )
        }
    }

    fun updateLabel(updatedLabel: String) {
        state = state.copy(
            label = updatedLabel
        )
    }

    fun entrySelected(entryType: EntryType, language: BIP39.WordListLanguage = BIP39.WordListLanguage.English) {
        state = when (entryType) {
            EntryType.MANUAL -> state.copy(enterWordUIState = EnterPhraseUIState.EDIT, currentLanguage = language)
            EntryType.PASTE -> state.copy(enterWordUIState = EnterPhraseUIState.PASTE_ENTRY)
        }
    }

    fun resetSubmitResourceErrorState() {
        state = state.copy(
            submitResource = Resource.Uninitialized,
            editedWordIndex = 0,
            enterWordUIState = EnterPhraseUIState.VIEW
        )
    }

    fun resetUserResourceAndRetryGetUserApiCall() {
        state = state.copy(userResource = Resource.Uninitialized)
        retrieveOwnerState()
    }

    fun onBackClicked() {
        state = when (state.enterWordUIState) {
            EnterPhraseUIState.DONE,
            EnterPhraseUIState.SELECT_ENTRY_TYPE -> state.copy(exitFlow = true)
            EnterPhraseUIState.PASTE_ENTRY -> {
                state.copy(
                    enterWordUIState = EnterPhraseUIState.SELECT_ENTRY_TYPE,
                    editedWord = "",
                    editedWordIndex = 0,
                    enteredWords = emptyList(),
                )
            }
            EnterPhraseUIState.SELECTED,
            EnterPhraseUIState.EDIT -> {
                if (state.editedWordIndex == 0 && state.enteredWords.isEmpty()) {
                    state.copy(
                        enterWordUIState = EnterPhraseUIState.SELECT_ENTRY_TYPE,
                        editedWord = ""
                    )
                } else {
                    val index = if (state.editedWordIndex >= state.enteredWords.size) {
                        state.editedWordIndex - 1
                    } else {
                        state.editedWordIndex
                    }

                    state.copy(
                        editedWordIndex = index,
                        editedWord = "",
                        enterWordUIState = EnterPhraseUIState.VIEW
                    )
                }
            }
            EnterPhraseUIState.VIEW ->
                state.copy(cancelInputSeedPhraseConfirmationDialog = true)

            EnterPhraseUIState.REVIEW ->
                 state.copy(
                    editedWord = "",
                    enterWordUIState = EnterPhraseUIState.VIEW,
                    editedWordIndex = 0
                )
            EnterPhraseUIState.LABEL ->
                state.copy(exitConfirmationDialog = true)
        }
    }

    fun userHasSeenPushDialog() = pushRepository.userHasSeenPushDialog()

    fun setUserSeenPushDialog(seenDialog: Boolean) =
        pushRepository.setUserSeenPushDialog(seenDialog)

    private fun checkUserHasRespondedToNotificationOptIn() {
        viewModelScope.launch {
            if (pushRepository.userHasSeenPushDialog()) {
                submitNotificationTokenForRegistration()
                state = state.copy(phraseEntryComplete = Resource.Success(Unit))
            } else {
                state = state.copy(
                    showPushNotificationsDialog = Resource.Success(Unit)
                )
            }
        }
    }

    private fun submitNotificationTokenForRegistration() {
        viewModelScope.launch {
            try {
                val token = pushRepository.retrievePushToken()
                if (token.isNotEmpty()) {
                    val pushBody = PushBody(
                        deviceType = PushRepositoryImpl.DEVICE_TYPE,
                        token = token
                    )
                    pushRepository.addPushNotification(pushBody = pushBody)
                }
            } catch (e: Exception) {
                e.sendError(CrashReportingUtil.SubmitNotificationToken)
            }
        }
    }

    fun finishPhraseEntry() {
        if (state.isSavingFirstSeedPhrase) {
            checkUserHasRespondedToNotificationOptIn()
        } else {
            state = state.copy(phraseEntryComplete = Resource.Success(Unit))
        }
    }
    fun finishPushNotificationDialog() {
        submitNotificationTokenForRegistration()
        state = state.copy(showPushNotificationsDialog = Resource.Uninitialized)
        state = state.copy(phraseEntryComplete = Resource.Success(Unit))
    }

    fun exitFlow() {
        state = state.copy(exitFlow = true)
    }

    fun resetExitFlow() {
        state = state.copy(exitFlow = false)
    }

    fun resetPhraseEntryComplete() {
        state = state.copy(phraseEntryComplete = Resource.Uninitialized)
    }

    fun onPhrasePasted(pastedPhrase: String) {
        val words =
            try {
                if (pastedPhrase.isEmpty()) {
                    listOf("Clipboard is empty...")
                } else {
                    BIP39.splitToWords(pastedPhrase)
                }
            } catch (t: Throwable) {
                Exception("Unable to split words").sendError(CrashReportingUtil.PastePhrase)
                listOf("Unable to create phrase...")
            }

        val editedWordIndex = if (words.size > 1) words.size - 1 else 0

        state = state.copy(
            enteredWords = words,
            editedWordIndex = editedWordIndex,
            editedWord = words[editedWordIndex],
            currentLanguage = BIP39.determineLanguage(words)
        )

        submitFullPhrase()
    }

    fun hideExitConfirmationDialog() {
        state = state.copy(exitConfirmationDialog = false)
    }

    fun hideCancelInputSeedPhraseConfirmationDialog() {
        state = state.copy(cancelInputSeedPhraseConfirmationDialog = false)
    }

    fun navigateToSeedPhraseType() {
        state = state.copy(
            enterWordUIState = EnterPhraseUIState.SELECT_ENTRY_TYPE,
            editedWordIndex = 0,
            editedWord = "",
            enteredWords = emptyList()
        )
    }
}