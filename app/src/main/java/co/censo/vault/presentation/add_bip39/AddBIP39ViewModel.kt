package co.censo.vault.presentation.add_bip39

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import co.censo.vault.CryptographyManager
import co.censo.vault.PhraseValidator
import co.censo.vault.Resource
import co.censo.vault.jsonMapper
import co.censo.vault.storage.EncryptedBIP39
import co.censo.vault.storage.SharedPrefsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZonedDateTime
import java.util.Base64
import javax.inject.Inject

@HiltViewModel
class AddBIP39ViewModel @Inject constructor(
    private val cryptographyManager: CryptographyManager
) : ViewModel() {

    var state by mutableStateOf(AddBIP39State())
        private set

    fun reset() {
        state = AddBIP39State()
    }

    fun updateName(name: String) {
        state = state.copy(name = name)
    }

    fun updateUserEnteredPhrase(userEnteredPhrase: String) {
        state = state.copy(
            userEnteredPhrase = userEnteredPhrase,
            userEnteredPhraseError = null
        )
    }

    fun canSubmit(): Boolean {
        return state.name.isNotBlank() && state.userEnteredPhrase.isNotBlank()
    }

    fun submit() {
        state = state.copy(submitStatus = Resource.Loading())

        if (PhraseValidator.isPhraseValid(state.userEnteredPhrase)) {
            val currentPhrases = SharedPrefsHelper.retrieveBIP39Phrases()

            if (currentPhrases.containsKey(state.name)) {
                state = state.copy(
                    nameError = "You already stored BIP39 phrase with this name",
                    submitStatus = Resource.Error()
                )
            } else {
                val newPhrase = EncryptedBIP39(
                    Base64.getEncoder().encodeToString(
                        cryptographyManager.encryptData(
                            jsonMapper.writeValueAsString(
                                PhraseValidator.format(state.userEnteredPhrase).split(" ")
                            )
                        )
                    ),
                    ZonedDateTime.now()
                )

                SharedPrefsHelper.saveBIP39Phrases(currentPhrases + mapOf(state.name to newPhrase))

                state = state.copy(submitStatus = Resource.Success(Unit))
            }
        } else {
            state = state.copy(
                userEnteredPhraseError = "Invalid BIP39",
                submitStatus = Resource.Error()
            )
        }
    }
}