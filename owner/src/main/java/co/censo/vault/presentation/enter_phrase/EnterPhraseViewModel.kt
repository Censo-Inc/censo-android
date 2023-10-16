package co.censo.vault.presentation.enter_phrase

import Base58EncodedMasterPublicKey
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import co.censo.shared.data.Resource
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.projectLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EnterPhraseViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository
) : ViewModel() {

    var state by mutableStateOf(EnterPhraseState())
        private set

    fun onStart(masterPublicKey: Base58EncodedMasterPublicKey) {
        state = state.copy(masterPublicKey = masterPublicKey)
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

        projectLog(message = "Inserting ${state.editedWord} in ${state.editedWordIndex}")

        val phrase = state.enteredWords.toMutableList()

        if (state.editedWordIndex >= state.enteredWords.size) {
            projectLog(message = "Adding word to the end of the list...")
            phrase.add(state.editedWord)
        } else {
            projectLog(message = "Editing the word in the list...")
            phrase[state.editedWordIndex] = state.editedWord
        }

        projectLog(message = "Words: ${state.enteredWords}")

        state = state.copy(
            enterWordUIState = EnterPhraseUIState.VIEW,
            enteredWords = phrase
        )
    }

    fun submitFullPhrase() {

        projectLog(message = "Phrase submitted: ${state.enteredWords}")

        val phraseValid = try {
            Mnemonics.MnemonicCode(state.enteredWords.joinToString(" ")).toSeed(validate = true)
            projectLog(message = "Seed valid")
            true
        } catch (e: Exception) {
            projectLog(message = "Seed invalid: $e")
            false
        }

        state = state.copy(
            validPhrase = phraseValid,
            enterWordUIState = EnterPhraseUIState.REVIEW
        )
    }

    fun enterNextWord() {
        projectLog(message = "Moving index to ${state.editedWordIndex + 1}")

        state = state.copy(
            editedWordIndex = state.editedWordIndex + 1,
            editedWord = "",
            enterWordUIState = EnterPhraseUIState.EDIT
        )
    }

    fun incrementEditIndex(): Boolean {
        val currentIndex = state.editedWordIndex

        return if (currentIndex != state.enteredWords.size - 1) {
            projectLog(message = "Incrementing index to ${state.editedWordIndex + 1}")
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
            projectLog(message = "Decrementing index to ${state.editedWordIndex - 1}")
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

    fun moveToNickname() {
        state = state.copy(
            enterWordUIState = EnterPhraseUIState.NICKNAME
        )
    }

    fun saveSeedPhrase() {
        viewModelScope.launch {
            if (state.masterPublicKey == null) {
                state = state.copy(
                    submitResource = Resource.Error(exception = Exception("Missing public key"))
                )
                return@launch
            }


            val response = ownerRepository.storeSecret(
                state.masterPublicKey!!,
                state.nickName.trim(),
                state.enteredWords.joinToString(" ").trim()
            )

            state = state.copy(
                submitResource = response.map { },
                phraseEntryComplete = Resource.Success(Unit)
            )
        }
        projectLog(message = "Save this seed phrase and send user back to start...")
    }

    fun editEntirePhrase() {
        projectLog(message = "Editing seed phrase, send user back to start...")
        state = state.copy(
            editedWord = state.enteredWords[0],
            editedWordIndex = 0,
            enterWordUIState = EnterPhraseUIState.VIEW
        )
    }

    fun updateNickname(updatedNickName: String) {
        state = state.copy(nickName = updatedNickName.trim())

    }

    fun entrySelected(entryType: EntryType) {
        state = when (entryType) {
            EntryType.MANUAL -> state.copy(enterWordUIState = EnterPhraseUIState.EDIT)
            EntryType.PASTE -> state.copy(enterWordUIState = EnterPhraseUIState.EDIT)
        }
    }

    fun setViewPhrase() {
        state = state.copy(
            editedWordIndex = 0,
            enterWordUIState = EnterPhraseUIState.VIEW
        )
    }

    fun onBackClicked() {
        when (state.enterWordUIState) {
            EnterPhraseUIState.SELECT_ENTRY_TYPE -> {}
            EnterPhraseUIState.EDIT -> {}
            EnterPhraseUIState.SELECTED -> {}
            EnterPhraseUIState.VIEW -> {}
            EnterPhraseUIState.REVIEW -> {}
            EnterPhraseUIState.NICKNAME -> {}
        }
    }

    fun resetPhraseEntryComplete() {
        state = state.copy(phraseEntryComplete = Resource.Uninitialized)
    }

}