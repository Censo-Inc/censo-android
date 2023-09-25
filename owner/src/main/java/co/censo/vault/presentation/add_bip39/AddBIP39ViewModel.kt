package co.censo.vault.presentation.add_bip39

import Base58EncodedMasterPublicKey
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.vault.data.PhraseValidator
import co.censo.shared.data.Resource
import co.censo.shared.data.repository.OwnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddBIP39ViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository
) : ViewModel() {

    var state by mutableStateOf(AddBIP39State())
        private set

    fun reset() {
        state = AddBIP39State()
    }

    fun updateName(name: String) {
        state = state.copy(name = name.lowercase())
    }

    fun updateUserEnteredPhrase(userEnteredPhrase: String) {
        state = state.copy(
            userEnteredPhrase = userEnteredPhrase.lowercase(),
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

        viewModelScope.launch {
            val response = ownerRepository.storeSecret(
                state.masterPublicKey!!,
                state.name.trim(),
                state.userEnteredPhrase.trim()
            )

            state = state.copy(submitStatus = response.map { })
        }
    }

    fun onStart(masterPublicKey: Base58EncodedMasterPublicKey) {
        state = state.copy(masterPublicKey = masterPublicKey)
    }
}