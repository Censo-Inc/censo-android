package co.censo.vault.presentation.bip_39_detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import co.censo.vault.data.cryptography.CryptographyManager
import co.censo.vault.data.Resource
import co.censo.vault.presentation.bip_39_detail.BIP39DetailState.Companion.CHANGE_AMOUNT
import co.censo.vault.presentation.bip_39_detail.BIP39DetailState.Companion.FIRST_WORD_INDEX
import co.censo.vault.data.storage.Storage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.serialization.json.Json
import java.util.Base64
import javax.inject.Inject
import kotlinx.serialization.decodeFromString

@HiltViewModel
class BIP39DetailViewModel @Inject constructor(
    private val cryptographyManager: CryptographyManager,
    private val storage: Storage
) : ViewModel() {

    var state by mutableStateOf(BIP39DetailState())
        private set

    fun onStart(bip39Name: String) {
        state = state.copy(name = bip39Name)
        triggerBiometry()
    }

    fun reset() {
        state = BIP39DetailState(name = state.name)
    }

    private fun triggerBiometry() {
        state = state.copy(
            bioPromptTrigger = Resource.Success(Unit),
        )
    }

    fun onBiometryApproved() {
        state = state.copy(bioPromptTrigger = Resource.Uninitialized)

        val phrases = storage.retrieveBIP39Phrases()

        val encryptedPhrase = phrases[state.name]

        val encryptedData = Base64.getDecoder().decode(encryptedPhrase?.base64)

        val decryptedPhrase = cryptographyManager.decryptData(encryptedData)

        val phraseAsList: List<String> = Json.decodeFromString(String(decryptedPhrase))

        state = state.copy(bip39Phrase = phraseAsList.joinToString(" "))
    }

    fun onBiometryFailed() {
        state = state.copy(bioPromptTrigger = Resource.Error())
    }

    fun wordIndexChanged(increasing: Boolean) {
        val wordIndex = handleWordIndexChanged(
            increasing = increasing,
            currentWordIndex = state.currentWordIndex
        )

        state = state.copy(currentWordIndex = wordIndex)
    }

    private fun handleWordIndexChanged(increasing: Boolean, currentWordIndex: Int): Int {
        var newWordIndex =
            if (increasing) {
                currentWordIndex + CHANGE_AMOUNT
            } else {
                currentWordIndex - CHANGE_AMOUNT
            }

        if (newWordIndex > state.lastWordIndex) {
            newWordIndex = FIRST_WORD_INDEX
        } else if (newWordIndex < FIRST_WORD_INDEX) {
            //We want to display the last 4 words
            newWordIndex = state.lastSetStartIndex
        }

        return newWordIndex
    }

}
