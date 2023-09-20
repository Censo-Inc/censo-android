package co.censo.vault.presentation.add_bip39

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import co.censo.vault.data.PhraseValidator
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.key.InternalDeviceKey
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.storage.EncryptedBIP39
import co.censo.shared.data.storage.Storage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import java.util.Base64
import javax.inject.Inject
import kotlinx.serialization.encodeToString

@HiltViewModel
class AddBIP39ViewModel @Inject constructor(
    private val storage: Storage,
    private val keyRepository: KeyRepository
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

        val deviceKey = keyRepository.retrieveInternalDeviceKey()
        val encryptedPhrase = deviceKey.encrypt(phraseAsJson.toByteArray(Charsets.UTF_8))
        val base64EncryptedPhrase = Base64.getEncoder().encodeToString(encryptedPhrase)

        val newPhrase = EncryptedBIP39(
            base64EncryptedPhrase,
            Clock.System.now()
        )

        storage.saveBIP39Phrases(currentPhrases + mapOf(state.name to newPhrase))

        state = state.copy(submitStatus = Resource.Success(Unit))
    }
}