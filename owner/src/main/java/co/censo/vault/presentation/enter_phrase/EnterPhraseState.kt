package co.censo.vault.presentation.enter_phrase

import Base58EncodedMasterPublicKey
import co.censo.shared.data.Resource
import co.censo.shared.data.repository.EncryptedSeedPhrase
import co.censo.shared.util.BIP39InvalidReason

data class EnterPhraseState(
    val masterPublicKey: Base58EncodedMasterPublicKey? = null,
    val enteredWords: List<String> = emptyList(),
    val editedWord: String = "",
    val editedWordIndex: Int = 0,
    val enterWordUIState: EnterPhraseUIState = EnterPhraseUIState.SELECT_ENTRY_TYPE,
    val phraseInvalidReason: BIP39InvalidReason? = null,
    val label: String = "",
    val encryptedSeedPhrase: EncryptedSeedPhrase? = null,
    val submitResource: Resource<Unit> = Resource.Uninitialized,
    val phraseEntryComplete: Resource<Unit> = Resource.Uninitialized,
    val welcomeFlow: Boolean = false,
    val exitConfirmationDialog: Boolean = false,
    val exitFlow: Boolean = false
) {

    val labelValid = label.isNotEmpty()

    val backArrowType = when (enterWordUIState) {
        EnterPhraseUIState.EDIT,
        EnterPhraseUIState.PASTE_ENTRY,
        EnterPhraseUIState.SELECTED,
        EnterPhraseUIState.REVIEW -> BackIconType.BACK

        EnterPhraseUIState.SELECT_ENTRY_TYPE,
        EnterPhraseUIState.VIEW,
        EnterPhraseUIState.LABEL -> BackIconType.CLOSE
    }

    val error = submitResource is Resource.Error
    val loading = submitResource is Resource.Loading
}

enum class EnterPhraseUIState {
    SELECT_ENTRY_TYPE, PASTE_ENTRY, EDIT, SELECTED, VIEW, REVIEW, LABEL
}

enum class BackIconType {
    CLOSE, BACK
}

enum class EntryType {
    MANUAL, PASTE
}