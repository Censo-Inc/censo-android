package co.censo.vault.presentation.add_bip39

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import co.censo.vault.CryptographyManager
import co.censo.vault.PhraseValidator
import co.censo.vault.Resource
import co.censo.vault.storage.EncryptedBIP39
import co.censo.vault.storage.Storage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.serialization.json.Json
import java.time.ZonedDateTime
import java.util.Base64
import javax.inject.Inject
import kotlinx.serialization.encodeToString

@HiltViewModel
class AddBIP39ViewModel @Inject constructor(
    private val cryptographyManager: CryptographyManager,
    private val storage: Storage
) : ViewModel() {

    var state by mutableStateOf(AddBIP39State())
        private set

    fun reset() {
        state = AddBIP39State()
    }

    fun updateName(name: String) {
        state = state.copy(name = name.lowercase().trim())
    }

    fun updateUserEnteredPhrase(userEnteredPhrase: String) {
        state = state.copy(
            userEnteredPhrase = userEnteredPhrase.lowercase().trim(),
            userEnteredPhraseError = null
        )
    }

    fun canSubmit(): Boolean {
        return state.nameValid && state.userEnteredPhrase.isNotBlank()
    }

    fun submit() {
        state = state.copy(submitStatus = Resource.Loading())

        if (!state.nameValid) {
            state = state.copy(
                nameError = "Please enter a valid name",
                submitStatus = Resource.Error()
            )
            return
        }

        if (!PhraseValidator.isPhraseValid(state.userEnteredPhrase)) {
            state = state.copy(
                userEnteredPhraseError = "Invalid BIP39",
                submitStatus = Resource.Error()
            )
            return
        }

        val currentPhrases = storage.retrieveBIP39Phrases()

        if (currentPhrases.containsKey(state.name)) {
            state = state.copy(
                nameError = "You already stored BIP39 phrase with this name",
                submitStatus = Resource.Error()
            )
            return
        }

        val phraseAsList = PhraseValidator.format(state.userEnteredPhrase).split(" ")
        val phraseAsJson = Json.encodeToString(phraseAsList)

        val encryptedPhrase = cryptographyManager.encryptData(phraseAsJson)
        val base64EncryptedPhrase = Base64.getEncoder().encodeToString(encryptedPhrase)

        val newPhrase = EncryptedBIP39(
            base64EncryptedPhrase,
            ZonedDateTime.now()
        )

        storage.saveBIP39Phrases(currentPhrases + mapOf(state.name to newPhrase))

        state = state.copy(submitStatus = Resource.Success(Unit))
    }
}