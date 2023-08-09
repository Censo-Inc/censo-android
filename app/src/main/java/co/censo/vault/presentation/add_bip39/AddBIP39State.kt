package co.censo.vault.presentation.add_bip39

import co.censo.vault.Resource

data class AddBIP39State(
    val name: String = "",
    val userEnteredPhrase: String = "",
    val nameError: String? = null,
    val userEnteredPhraseError: String? = null,
    val submitStatus: Resource<Unit> = Resource.Uninitialized
)