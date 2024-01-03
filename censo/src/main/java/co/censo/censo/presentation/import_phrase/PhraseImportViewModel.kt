package co.censo.censo.presentation.import_phrase

import Base64EncodedData

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.censo.presentation.Screen
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.hexStringToByteArray
import co.censo.shared.data.model.Import
import co.censo.shared.data.model.ImportState
import co.censo.shared.data.model.ImportedPhrase
import co.censo.shared.data.model.OwnerProof
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.networking.IgnoreKeysJson.baseKotlinXJson
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.BIP39
import co.censo.shared.util.VaultCountDownTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.novacrypto.base58.Base58
import kotlinx.coroutines.launch
import java.util.Base64
import javax.inject.Inject

@HiltViewModel
class PhraseImportViewModel @Inject constructor(
    val keyRepository: KeyRepository,
    val ownerRepository: OwnerRepository,
    private val countDownTimer: VaultCountDownTimer
) : ViewModel() {

    var state by mutableStateOf(PhraseImportState())
        private set

    private fun startTimer(channel: String) {
        countDownTimer.start(5000) {
            checkForCompletedImport(channel)
        }
    }

    fun kickOffPhraseImport(importToAccept: Import) {
        viewModelScope.launch {
            val ownerStateResource = ownerRepository.retrieveUser().map { it.ownerState }

            state = state.copy(userResponse = ownerStateResource)


            if (ownerStateResource is Resource.Success) {
                acceptImport(importToAccept)
            }
        }
    }

    fun onStop() {
        countDownTimer.stop()
    }

    fun acceptImport(importToAccept: Import) {
        viewModelScope.launch {
            try {
                val deviceKey = keyRepository.retrieveInternalDeviceKey()

                val ownerProofSignature =
                    deviceKey.sign(
                        Base58.base58Decode(importToAccept.importKey.value)
                    )

                val base64Signature = Base64EncodedData(
                    Base64.getEncoder().encodeToString(ownerProofSignature)
                )

                val acceptImportResponse =
                    ownerRepository.acceptImport(
                        channel = importToAccept.channel(),
                        ownerProof = OwnerProof(
                            signature = base64Signature
                        )
                    )

                if (acceptImportResponse is Resource.Success) {
                    state = state.copy(
                        importPhraseState = ImportPhase.Completing(importToAccept)
                    )
                    startTimer(importToAccept.channel())
                }

                state = state.copy(acceptImportResource = acceptImportResponse)
            } catch (e: Exception) {
                state = state.copy(acceptImportResource = Resource.Error(exception = e))
            }
        }
    }

    private fun checkForCompletedImport(channel: String) {
        viewModelScope.launch {
            val response = ownerRepository.checkForCompletedImport(channel)

            if (response is Resource.Success) {
                response.data?.let {
                    when (val importState = it.importState) {
                        is ImportState.Completed -> {
                            val deviceKey = keyRepository.retrieveInternalDeviceKey()
                            val decryptedData = deviceKey.decrypt(importState.encryptedData.bytes)

                            val importedPhrase: ImportedPhrase =
                                baseKotlinXJson.decodeFromString(decryptedData.toString(Charsets.UTF_8))

                            val masterPublicKey =
                                (state.userResponse.data as OwnerState.Ready).vault.publicMasterEncryptionKey

                            val words = BIP39.binaryDataToWords(
                                binaryData = importedPhrase.binaryPhrase.value.hexStringToByteArray(),
                                language = importedPhrase.language,
                                importedSize = true
                            )

                            val route = Screen.EnterPhraseRoute.buildNavRoute(
                                masterPublicKey = masterPublicKey,
                                welcomeFlow = true,
                                words = words,
                                importingPhrase = true
                            )

                            state =
                                state.copy(
                                    importPhraseState = ImportPhase.Completed(importedPhrase),
                                    sendToSeedVerification = Resource.Success(route)
                                )
                        }

                        else -> {
                            //We will continue polling until completed so no need to respond to other states
                            //TODO: what would happen if the user rejected the share?
                        }
                    }
                }
            }

            state = state.copy(getEncryptedResponse = response)
        }
    }

    fun resetGetEncryptedResponse() {
        state = state.copy(getEncryptedResponse = Resource.Uninitialized)
    }

    fun exitFlow() {
        state = state.copy(exitFlow = Resource.Success(Unit))
    }

    fun resetExitFlow() {
        state = state.copy(exitFlow = Resource.Uninitialized)
    }

    fun resetSendVerification() {
        state = state.copy(sendToSeedVerification = Resource.Uninitialized)
    }
}