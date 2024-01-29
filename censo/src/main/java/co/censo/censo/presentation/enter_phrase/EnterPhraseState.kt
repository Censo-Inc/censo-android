package co.censo.censo.presentation.enter_phrase

import Base58EncodedMasterPublicKey
import Base64EncodedData
import ParticipantId
import android.graphics.Bitmap
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GetOwnerUserApiResponse
import co.censo.shared.data.model.InitialKeyData
import co.censo.shared.data.repository.EncryptedSeedPhrase
import co.censo.shared.presentation.cloud_storage.CloudStorageActionData
import co.censo.shared.util.BIP39
import co.censo.shared.util.BIP39InvalidReason

data class EnterPhraseState(
    val masterPublicKey: Base58EncodedMasterPublicKey? = null,
    val masterKeySignature: Base64EncodedData? = null,
    val enteredWords: List<String> = emptyList(),
    val editedWord: String = "",
    val editedWordIndex: Int = 0,
    val enterWordUIState: EnterPhraseUIState = EnterPhraseUIState.SELECT_ENTRY_TYPE,
    val currentLanguage: BIP39.WordListLanguage = BIP39.WordListLanguage.English,
    val label: String = "",
    val labelTooLong: String? = null,
    val seedPhraseType: SeedPhraseType = SeedPhraseType.TEXT,
    val encryptedSeedPhrase: EncryptedSeedPhrase? = null,
    val existingPhraseCount: Int = 0,
    val desiredGeneratedPhraseLength: BIP39.WordCount = BIP39.WordCount.TwentyFour,

    //Image
    val imageBitmap: Bitmap? = null,

    //Async
    val submitResource: Resource<Unit> = Resource.Uninitialized,
    val phraseEntryComplete: Resource<Unit> = Resource.Uninitialized,
    val userResource: Resource<GetOwnerUserApiResponse> = Resource.Uninitialized,
    val phraseEncryptionInProgress: Boolean = false,
    val triggerPaywallUI: Resource<Unit> = Resource.Uninitialized,
    val imageCaptureResource: Resource<Unit> = Resource.Uninitialized,

    //Flags
    val welcomeFlow: Boolean = false,
    val cancelInputSeedPhraseConfirmationDialog: Boolean = false,
    val exitConfirmationDialog: Boolean = false,
    val exitFlow: Boolean = false,
    val showInvalidPhraseDialog: Resource<BIP39InvalidReason> = Resource.Uninitialized,

    // Cloud Storage
    val cloudStorageAction: CloudStorageActionData = CloudStorageActionData(),
    val loadKeyInProgress: Resource<Unit> = Resource.Uninitialized,
    val ownerApproverParticipantId: ParticipantId? = null,

    val keyData: InitialKeyData? = null,

    val userHasOwnPhrase: Boolean = false,
    val triggerDeleteUserDialog: Resource<Unit> = Resource.Uninitialized,
    val deleteUserResource: Resource<Unit> = Resource.Uninitialized,
) {

    companion object {
        const val PHRASE_LABEL_MAX_LENGTH = 50
    }

    val error = submitResource is Resource.Error
            || userResource is Resource.Error
            || imageCaptureResource is Resource.Error

    val loading = submitResource is Resource.Loading
            || userResource is Resource.Loading
            || phraseEncryptionInProgress
            || loadKeyInProgress is Resource.Loading
            || deleteUserResource is Resource.Loading


    val labelIsTooLong = label.length > PHRASE_LABEL_MAX_LENGTH
    val labelValid = label.isNotEmpty() && !labelIsTooLong

    val backArrowType = when (enterWordUIState) {
        EnterPhraseUIState.SELECT_ENTRY_TYPE_OWN,
        EnterPhraseUIState.EDIT,
        EnterPhraseUIState.LABEL,
        EnterPhraseUIState.SELECTED,
        EnterPhraseUIState.GENERATE,
        EnterPhraseUIState.PASTE_ENTRY,
        EnterPhraseUIState.CAPTURE_IMAGE,
        EnterPhraseUIState.REVIEW_IMAGE,
        EnterPhraseUIState.REVIEW_WORDS -> BackIconType.BACK

        EnterPhraseUIState.SELECT_ENTRY_TYPE,
        EnterPhraseUIState.VIEW,
        EnterPhraseUIState.DONE,
        EnterPhraseUIState.NOTIFICATIONS -> BackIconType.CLOSE
    }
}

enum class EnterPhraseUIState {
    SELECT_ENTRY_TYPE,
    SELECT_ENTRY_TYPE_OWN,
    CAPTURE_IMAGE,
    PASTE_ENTRY,
    EDIT,
    GENERATE,
    SELECTED,
    VIEW,
    REVIEW_WORDS,
    REVIEW_IMAGE,
    LABEL,
    DONE,
    NOTIFICATIONS,
}

enum class BackIconType {
    CLOSE, BACK
}

enum class EntryType {
    MANUAL, PASTE, GENERATE, IMPORT, IMAGE
}

enum class SeedPhraseType {
    TEXT, IMAGE
}