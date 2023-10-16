package co.censo.vault.presentation.enter_phrase

import Base58EncodedMasterPublicKey
import co.censo.shared.data.Resource

data class EnterPhraseState(
    val masterPublicKey: Base58EncodedMasterPublicKey? = null,
    val enteredWords: List<String> = emptyList(),
    val editedWord: String = "",
    val editedWordIndex: Int = 0,
    val enterWordUIState: EnterPhraseUIState = EnterPhraseUIState.SELECT_ENTRY_TYPE,
    val validPhrase: Boolean = false,
    val nickName: String = "",
    val submitResource: Resource<Unit> = Resource.Uninitialized,
    val phraseEntryComplete: Resource<Unit> = Resource.Uninitialized
    ) {

    val validName = nickName.isNotEmpty()

    val backArrowType = when (enterWordUIState) {
        EnterPhraseUIState.NICKNAME,
        EnterPhraseUIState.EDIT,
        EnterPhraseUIState.SELECTED -> BackIconType.BACK
        EnterPhraseUIState.SELECT_ENTRY_TYPE,
        EnterPhraseUIState.VIEW,
        EnterPhraseUIState.REVIEW -> BackIconType.CLOSE
    }

    val error = submitResource is Resource.Error
    val loading = submitResource is Resource.Loading
}

enum class EnterPhraseUIState {
    SELECT_ENTRY_TYPE, EDIT, SELECTED, VIEW, REVIEW, NICKNAME
}

enum class BackIconType {
    CLOSE, BACK
}

enum class EntryType {
    MANUAL, PASTE
}