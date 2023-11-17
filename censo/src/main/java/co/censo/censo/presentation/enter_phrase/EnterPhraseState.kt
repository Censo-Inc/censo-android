package co.censo.censo.presentation.enter_phrase

import Base58EncodedMasterPublicKey
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GetOwnerUserApiResponse
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
    val labelTooLong: String? = null,
    val encryptedSeedPhrase: EncryptedSeedPhrase? = null,

    //Async
    val submitResource: Resource<Unit> = Resource.Uninitialized,
    val phraseEntryComplete: Resource<Unit> = Resource.Uninitialized,
    val userResource: Resource<GetOwnerUserApiResponse> = Resource.Uninitialized,

    //Flags
    val welcomeFlow: Boolean = false,
    val exitConfirmationDialog: Boolean = false,
    val exitFlow: Boolean = false,
    val isSavingFirstSeedPhrase: Boolean = false,
    val showPushNotificationsDialog: Resource<Unit> = Resource.Uninitialized
) {

    companion object {
        const val PHRASE_LABEL_MAX_LENGTH = 50
    }

    val labelIsTooLong = label.length > PHRASE_LABEL_MAX_LENGTH
    val labelValid = label.isNotEmpty() && !labelIsTooLong

    val backArrowType = when (enterWordUIState) {
        EnterPhraseUIState.EDIT,
        EnterPhraseUIState.LABEL,
        EnterPhraseUIState.SELECTED,
        EnterPhraseUIState.REVIEW -> BackIconType.BACK

        EnterPhraseUIState.SELECT_ENTRY_TYPE,
        EnterPhraseUIState.PASTE_ENTRY,
        EnterPhraseUIState.VIEW,
        EnterPhraseUIState.DONE-> BackIconType.CLOSE
    }

    val error = submitResource is Resource.Error || userResource is Resource.Error
    val loading = submitResource is Resource.Loading || userResource is Resource.Loading
}

enum class EnterPhraseUIState {
    SELECT_ENTRY_TYPE, PASTE_ENTRY, EDIT, SELECTED, VIEW, REVIEW, LABEL, DONE
}

enum class BackIconType {
    CLOSE, BACK
}

enum class EntryType {
    MANUAL, PASTE
}