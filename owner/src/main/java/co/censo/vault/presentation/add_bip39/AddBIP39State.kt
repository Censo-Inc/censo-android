package co.censo.vault.presentation.add_bip39

import Base58EncodedMasterPublicKey
import co.censo.shared.data.Resource

data class AddBIP39State(
    val name: String = "",
    val userEnteredPhrase: String = "",
    val nameError: String? = null,
    val userEnteredPhraseError: String? = null,
    val submitStatus: Resource<Unit> = Resource.Uninitialized,
    val masterPublicKey: Base58EncodedMasterPublicKey = Base58EncodedMasterPublicKey("")
) {
    val nameValid = name.isNotEmpty()
}