package co.censo.censo.presentation.enter_phrase

import Base58EncodedApproverPublicKey
import Base58EncodedMasterPublicKey
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.decryptWithEntropy
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.model.InitialKeyData
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.networking.PushBody
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.data.repository.PushRepository
import co.censo.shared.data.repository.PushRepositoryImpl
import co.censo.shared.presentation.cloud_storage.CloudStorageActionData
import co.censo.shared.presentation.cloud_storage.CloudStorageActions
import co.censo.shared.util.BIP39
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.sendError
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.novacrypto.base58.Base58
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import java.lang.Integer.max
import java.lang.Integer.min
import java.util.Base64
import javax.inject.Inject

@HiltViewModel
class EnterPhraseViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository,
    private val ownerStateFlow: MutableStateFlow<Resource<OwnerState>>,
    private val pushRepository: PushRepository,
    ) : ViewModel() {

    var state by mutableStateOf(EnterPhraseState())
        private set

    fun onStart(
        welcomeFlow: Boolean,
        masterPublicKey: Base58EncodedMasterPublicKey
    ) {
        state = state.copy(
            welcomeFlow = welcomeFlow,
            masterPublicKey = masterPublicKey
        )

        retrieveOwnerState()
    }

    //Only retrieving owner state to determine if this is the first seed phrase they are saving
    fun retrieveOwnerState() {
        state = state.copy(userResource = Resource.Loading())
        viewModelScope.launch {
            val response = ownerRepository.retrieveUser()

            state = state.copy(userResource = response)

            if (response is Resource.Success) {
                val ownerState = response.data!!.ownerState
                ownerStateFlow.emit(Resource.Success(ownerState))
                if (ownerState is OwnerState.Ready) {
                    projectLog(message = "Assigning masterKeySig and ownerApproverPartiID to state")
                    state = state.copy(
                        isSavingFirstSeedPhrase = ownerState.vault.seedPhrases.isEmpty(),
                        ownerApproverParticipantId = ownerState.policy.owner?.participantId,
                        masterKeySignature = ownerState.policy.masterKeySignature
                    )
                }
            }
        }
    }

    fun updateEditedWord(updatedWord: String) {
        state = state.copy(editedWord = updatedWord.lowercase().trim())
    }

    fun wordSelected(updatedWord: String) {
        state = state.copy(
            editedWord = updatedWord,
            enterWordUIState = EnterPhraseUIState.SELECTED
        )
    }

    fun wordSubmitted() {
        val phrase = state.enteredWords.toMutableList()

        if (state.editedWordIndex >= state.enteredWords.size) {
            phrase.add(state.editedWord)
        } else {
            phrase[state.editedWordIndex] = state.editedWord
        }

        state = state.copy(
            enterWordUIState = EnterPhraseUIState.VIEW,
            enteredWords = phrase
        )
    }

    fun submitFullPhrase() {
        val phraseInvalidReason = BIP39.validateSeedPhrase(state.enteredWords)

        state = phraseInvalidReason?.let {
            state.copy(showInvalidPhraseDialog = Resource.Success(it))
        } ?: state.copy(
            enterWordUIState = EnterPhraseUIState.REVIEW
        )
    }

    fun enterNextWord() {
        state = state.copy(
            editedWordIndex = state.editedWordIndex + 1,
            editedWord = "",
            enterWordUIState = EnterPhraseUIState.EDIT
        )
    }

    fun incrementEditIndex(): Boolean {
        val currentIndex = state.editedWordIndex

        return if (currentIndex != state.enteredWords.size - 1
            && state.enteredWords.isNotEmpty()
            && currentIndex <= state.enteredWords.size - 1
        ) {
            state =
                state.copy(
                    editedWordIndex = state.editedWordIndex + 1,
                    editedWord = state.enteredWords[state.editedWordIndex + 1]
                )
            true
        } else {
            false
        }
    }

    fun decrementEditIndex(): Boolean {
        val currentIndex = state.editedWordIndex

        return if (currentIndex != 0) {
            state =
                state.copy(
                    editedWordIndex = state.editedWordIndex - 1,
                    editedWord = state.enteredWords[state.editedWordIndex - 1]
                )
            true
        } else {
            false
        }
    }

    fun editExistingWord() {
        state = state.copy(enterWordUIState = EnterPhraseUIState.EDIT)
    }

    fun deleteExistingWord() {
        val newEnteredWords = state.enteredWords.filterIndexed { ix, _ -> ix != state.editedWordIndex }
        state = if (newEnteredWords.isEmpty()) {
            state.copy(
                enteredWords = emptyList(),
                editedWordIndex = 0,
                enterWordUIState = EnterPhraseUIState.EDIT,
                editedWord = ""
            )
        } else {
            state.copy(
                enteredWords = newEnteredWords,
                editedWordIndex = max(0, min(state.editedWordIndex, newEnteredWords.size - 1))
            )
        }
    }

    fun moveToLabel() {
        if (!state.phraseEncryptionInProgress) {
            state = state.copy(phraseEncryptionInProgress = true)

            if (state.masterPublicKey == null) {
                state = state.copy(
                    submitResource = Resource.Error(exception = Exception("Missing public key")),
                    phraseEncryptionInProgress = false
                )
                return
            }

            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    // encrypt seed phrase and drop single words
                    val encryptedSeedPhrase = ownerRepository.encryptSeedPhrase(
                        state.masterPublicKey!!,
                        state.enteredWords
                    )

                    state = state.copy(
                        enterWordUIState = EnterPhraseUIState.LABEL,
                        encryptedSeedPhrase = encryptedSeedPhrase,
                        enteredWords = listOf(),
                        phraseEncryptionInProgress = false
                    )
                }.onFailure { _ ->
                    state = state.copy(
                        submitResource = Resource.Error(exception = Exception("Unable to encrypt seed phrase")),
                        phraseEncryptionInProgress = false
                    )
                }
            }
        }
    }

    fun saveSeedPhrase() {
        projectLog(message = "Checking for masterKeySignature")
        //We only need to verify master key sig if there is one
        val masterKeySignature = state.masterKeySignature
        if (masterKeySignature != null) {
            projectLog(message = "Triggering key download to verify master key signature")
            //Load the key from the cloud
            triggerKeyDownload()
        } else {
            projectLog(message = "Storing seed phrase without verifying master key signature")
            storeSeedPhrase(shouldVerifyMasterKeySignature = false)
        }

    }

    private fun triggerKeyDownload() {
        projectLog(message = "Triggering key download")
        state = state.copy(
            cloudStorageAction = CloudStorageActionData(
                triggerAction = true, action = CloudStorageActions.DOWNLOAD,
            ),
            loadKeyInProgress = Resource.Loading()
        )
    }

    private fun verifyMasterKeySignature() : Boolean {
        projectLog(message = "Verifying master key signature")
        val ownerPolicy = (ownerStateFlow.value.data as OwnerState.Ready).policy

        projectLog(message = "Grabbing master public key and master key sig from local state")
        val masterPublicKey = state.masterPublicKey
        val masterKeySignature = state.masterKeySignature

        projectLog(message = "null checking master public key + master key sig")
        if (masterKeySignature != null && masterPublicKey != null) {
            val deviceKeyId = keyRepository.retrieveSavedDeviceId()
            val entropy = ownerPolicy.ownerEntropy
            projectLog(message = "Got entropy and deviceKeyId")

            val ownerApproverKeyData = state.keyData
            return if (ownerApproverKeyData != null) {
                projectLog(message = "Recreating keyData into EncryptionKey")
                val ownerApproverEncryptionKey = EncryptionKey.generateFromPrivateKeyRaw(
                    ownerApproverKeyData.encryptedPrivateKey.decryptWithEntropy(
                        deviceKeyId, entropy
                    ).bigInt()
                )

                projectLog(message = "Verifying master public key against master key sig, with ownerApproverEncryptionKey")
                ownerApproverEncryptionKey.verify(
                    signedData = (masterPublicKey.ecPublicKey as BCECPublicKey).q.getEncoded(false),
                    signature = masterKeySignature.bytes
                )
            } else {
                projectLog(message = "owner approver key data null")
                false
            }
        } else {
            projectLog(message = "master public key or master key sig is null")
            return false
        }
    }

    private fun storeSeedPhrase(shouldVerifyMasterKeySignature: Boolean) {
        state = state.copy(submitResource = Resource.Loading())

        viewModelScope.launch {

            if (shouldVerifyMasterKeySignature && !verifyMasterKeySignature()) {
                projectLog(message = "Master key sig verification failed")
                state = state.copy(
                    submitResource = Resource.Error(
                        exception = Exception("Unable to verify data, please try again")
                    )
                )
                return@launch
            }

            if (shouldVerifyMasterKeySignature) {
                projectLog(message = "Verified master key sig succesfully")
            }

            projectLog(message = "Moving forward with storing seed phrase")

            val response = ownerRepository.storeSeedPhrase(
                state.label.trim(),
                state.encryptedSeedPhrase!!
            )

            state = state.copy(
                submitResource = response.map { },
                enterWordUIState = EnterPhraseUIState.DONE
            )

            if (response is Resource.Success) {
                ownerStateFlow.tryEmit(response.map { it.ownerState })
            }
        }
    }

    fun editEntirePhrase() {
        state = if (state.enteredWords.isEmpty()) {
            state.copy(enterWordUIState = EnterPhraseUIState.SELECT_ENTRY_TYPE)
        } else {
            state.copy(
                editedWord = state.enteredWords[0],
                editedWordIndex = 0,
                enterWordUIState = EnterPhraseUIState.VIEW
            )
        }
    }

    fun updateLabel(updatedLabel: String) {
        state = state.copy(
            label = updatedLabel
        )
    }

    fun entrySelected(entryType: EntryType, language: BIP39.WordListLanguage = BIP39.WordListLanguage.English) {
        state = when (entryType) {
            EntryType.MANUAL -> state.copy(enterWordUIState = EnterPhraseUIState.EDIT, currentLanguage = language)
            EntryType.PASTE -> state.copy(enterWordUIState = EnterPhraseUIState.PASTE_ENTRY)
            EntryType.GENERATE -> state.copy(enterWordUIState = EnterPhraseUIState.GENERATE)
        }
    }

    fun resetSubmitResourceErrorState() {
        state = state.copy(
            submitResource = Resource.Uninitialized,
            editedWordIndex = 0,
            enterWordUIState = EnterPhraseUIState.VIEW
        )
    }

    fun resetUserResourceAndRetryGetUserApiCall() {
        state = state.copy(userResource = Resource.Uninitialized)
        retrieveOwnerState()
    }

    fun onBackClicked() {
        state = when (state.enterWordUIState) {
            EnterPhraseUIState.DONE,
            EnterPhraseUIState.NOTIFICATIONS,
            EnterPhraseUIState.SELECT_ENTRY_TYPE -> state.copy(exitFlow = true)
            EnterPhraseUIState.PASTE_ENTRY, EnterPhraseUIState.GENERATE -> {
                state.copy(
                    enterWordUIState = EnterPhraseUIState.SELECT_ENTRY_TYPE,
                    editedWord = "",
                    editedWordIndex = 0,
                    enteredWords = emptyList(),
                )
            }
            EnterPhraseUIState.SELECTED,
            EnterPhraseUIState.EDIT -> {
                if (state.editedWordIndex == 0 && state.enteredWords.isEmpty()) {
                    state.copy(
                        enterWordUIState = EnterPhraseUIState.SELECT_ENTRY_TYPE,
                        editedWord = ""
                    )
                } else {
                    val index = if (state.editedWordIndex >= state.enteredWords.size) {
                        state.editedWordIndex - 1
                    } else {
                        state.editedWordIndex
                    }

                    state.copy(
                        editedWordIndex = index,
                        editedWord = "",
                        enterWordUIState = EnterPhraseUIState.VIEW
                    )
                }
            }
            EnterPhraseUIState.VIEW ->
                state.copy(cancelInputSeedPhraseConfirmationDialog = true)

            EnterPhraseUIState.REVIEW ->
                 state.copy(
                    editedWord = "",
                    enterWordUIState = EnterPhraseUIState.VIEW,
                    editedWordIndex = 0
                )
            EnterPhraseUIState.LABEL ->
                state.copy(exitConfirmationDialog = true)
        }
    }

    fun userHasSeenPushDialog() = pushRepository.userHasSeenPushDialog()

    fun setUserSeenPushDialog(seenDialog: Boolean) =
        pushRepository.setUserSeenPushDialog(seenDialog)

    fun finishPhraseEntry() {
        state = if (state.isSavingFirstSeedPhrase) {
            state.copy(enterWordUIState = EnterPhraseUIState.NOTIFICATIONS)
        } else {
            state.copy(phraseEntryComplete = Resource.Success(Unit))
        }
    }

    fun finishPushNotificationScreen() {
        state = state.copy(phraseEntryComplete = Resource.Success(Unit))
    }

    fun exitFlow() {
        state = state.copy(exitFlow = true)
    }

    fun resetExitFlow() {
        state = state.copy(exitFlow = false)
    }

    fun resetPhraseEntryComplete() {
        state = state.copy(phraseEntryComplete = Resource.Uninitialized)
    }

    fun onPhrasePasted(pastedPhrase: String) {
        val words =
            try {
                if (pastedPhrase.isEmpty()) {
                    listOf("Clipboard is empty...")
                } else {
                    BIP39.splitToWords(pastedPhrase)
                }
            } catch (t: Throwable) {
                Exception("Unable to split words").sendError(CrashReportingUtil.PastePhrase)
                listOf("Unable to create phrase...")
            }

        val editedWordIndex = if (words.size > 1) words.size - 1 else 0

        state = state.copy(
            enteredWords = words,
            editedWordIndex = editedWordIndex,
            editedWord = words[editedWordIndex],
            currentLanguage = BIP39.determineLanguage(words)
        )

        submitFullPhrase()
    }

    fun onDesiredGeneratedPhraseLengthSelected(wordCount: BIP39.WordCount) {
        state = state.copy(
            desiredGeneratedPhraseLength = wordCount
        )
    }

    fun generatePhrase() {
        state = state.copy(
            enteredWords = BIP39.generate(state.desiredGeneratedPhraseLength, state.currentLanguage),
        )
        submitFullPhrase()
    }

    fun hideExitConfirmationDialog() {
        state = state.copy(exitConfirmationDialog = false)
    }

    fun hideCancelInputSeedPhraseConfirmationDialog() {
        state = state.copy(cancelInputSeedPhraseConfirmationDialog = false)
    }

    fun navigateToSeedPhraseType() {
        state = state.copy(
            enterWordUIState = EnterPhraseUIState.SELECT_ENTRY_TYPE,
            editedWordIndex = 0,
            editedWord = "",
            enteredWords = emptyList()
        )
    }

    fun removeInvalidPhraseDialog() {
        state = state.copy(showInvalidPhraseDialog = Resource.Uninitialized)
    }

    //region Cloud Storage
    fun onKeyDownloadSuccess(encryptedKey: ByteArray) {
        projectLog(message = "Key downloaded successfully")
        resetCloudStorageActionState()

        //TODO: Use a mock here to get entropy from sharedPrefs//Test locally for now
        val entropy = (ownerStateFlow.value.data as OwnerState.Ready).policy.ownerEntropy
        projectLog(message = "Entropy from ownerState: $entropy")
        val deviceId = keyRepository.retrieveSavedDeviceId()

        //TODO: Should wrap in try catch
        val publicKey =
            Base58EncodedApproverPublicKey(
                encryptedKey.decryptWithEntropy(
                    deviceKeyId = deviceId,
                    entropy = entropy
                ).toEncryptionKey().publicExternalRepresentation().value
            )

        projectLog(message = "Setting recreated key data to state")
        state = state.copy(
            keyData = InitialKeyData(
                encryptedPrivateKey = encryptedKey,
                publicKey = publicKey
            )
        )

        projectLog(message = "Recreated key data")
        storeSeedPhrase(shouldVerifyMasterKeySignature = true)
    }

    fun onKeyDownloadFailed(exception: Exception?) {
        projectLog(message = "Key download failed")
        resetCloudStorageActionState()
        state = state.copy(submitResource = Resource.Error(exception = exception))
        exception?.sendError(CrashReportingUtil.CloudDownload)
    }
    //endregion

    //region Reset methods
    private fun resetCloudStorageActionState() {
        projectLog(message = "Resetting cloud storage action state")
        state = state.copy(
            cloudStorageAction = CloudStorageActionData(),
            loadKeyInProgress = Resource.Uninitialized
        )
    }
    //endregion

}