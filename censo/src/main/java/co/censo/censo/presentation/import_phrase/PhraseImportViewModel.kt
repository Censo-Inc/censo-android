package co.censo.censo.presentation.import_phrase

import Base64EncodedData
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.censo.presentation.Screen
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.ECHelper.convertRawSignatureToDerFormat
import co.censo.shared.data.cryptography.ECPublicKeyDecoder.recreateECPublicKey
import co.censo.shared.data.cryptography.key.ExternalEncryptionKey
import co.censo.shared.data.cryptography.sha256digest
import co.censo.shared.data.model.Import
import co.censo.shared.data.model.ImportState
import co.censo.shared.data.model.OwnerProof
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.VaultCountDownTimer
import co.censo.shared.util.sendError
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.novacrypto.base58.Base58
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
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

    private fun startTimer(importToAccept: Import) {
        countDownTimer.start(2000) {
            val importErrorType = isLinkExpired(importToAccept.timestamp)
            if (importErrorType != ImportErrorType.NONE) {
                countDownTimer.stop()
                state = state.copy(importErrorType = importErrorType)
            } else {
                checkForCompletedImport(importToAccept.channel())
            }
        }
    }

    fun kickOffPhraseImport(importToAccept: Import) {
        viewModelScope.launch {
            state = state.copy(importPhraseState = ImportPhase.Accepting)
            val ownerStateResource = ownerRepository.retrieveUser().map { it.ownerState }

            state = state.copy(userResponse = ownerStateResource)

            if (ownerStateResource is Resource.Success) {
                if (ownerStateResource.data !is OwnerState.Ready) {
                    state = state.copy(userResponse = Resource.Error())
                } else {
                    acceptImport(importToAccept)
                }
            }
        }
    }

    fun onStart(importToAccept: Import) {
        verifySignature(importToAccept)
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
                    startTimer(importToAccept)
                }

                state = state.copy(acceptImportResource = acceptImportResponse)
            } catch (e: Exception) {
                state = state.copy(acceptImportResource = Resource.Error(exception = e))
            }
        }
    }

    private fun verifySignature(import: Import): Boolean {
        return try {
            val publicKey = recreateECPublicKey(import.importKey.getBytes())
            val key = ExternalEncryptionKey(publicKey)

            val nameAsUTF8 = String(Base64EncodedData(import.name).bytes, Charsets.UTF_8)

            val timestampBytes = import.timestamp.toString().toByteArray(Charsets.UTF_8)
            val nameBytes = nameAsUTF8.toByteArray(Charsets.UTF_8).sha256digest()

            val signedData = ByteBuffer.allocate(timestampBytes.size + nameBytes.size).apply {
                put(timestampBytes)
                put(nameBytes)
            }.array()

            val derSignature = if (import.signature.bytes.size > 64) {
                import.signature.bytes
            } else {
                convertRawSignatureToDerFormat(import.signature.bytes)
            }

            val verified = key.verify(
                signedData = signedData,
                signature = derSignature
            )

            if (verified) {
                val importErrorType = isLinkExpired(import.timestamp)

                if (importErrorType == ImportErrorType.NONE) {
                    true
                } else {
                    state = state.copy(importErrorType = importErrorType)
                    false
                }
            } else {
                state = state.copy(importErrorType = ImportErrorType.BAD_SIGNATURE)
                false
            }
        } catch (e: Exception) {
            e.sendError(CrashReportingUtil.ImportPhrase)
            state = state.copy(importErrorType = ImportErrorType.BAD_SIGNATURE)
            false
        }
    }

    private fun checkForCompletedImport(channel: String) {
        viewModelScope.launch {
            val response = ownerRepository.checkForCompletedImport(channel)

            if (response is Resource.Success) {
                response.data?.let {
                    when (val importState = it.importState) {
                        is ImportState.Completed -> {
                            val masterPublicKey =
                                (state.userResponse.data as? OwnerState.Ready)?.vault?.publicMasterEncryptionKey

                            if (masterPublicKey == null) {
                                state = state.copy(userResponse = Resource.Error())
                                return@launch
                            }

                            val base64UrlEncodedData =
                                Base64.getUrlEncoder().encodeToString(
                                    Base64.getDecoder()
                                        .decode(importState.encryptedData.base64Encoded)
                                )

                            val route = Screen.EnterPhraseRoute.buildNavRoute(
                                masterPublicKey = masterPublicKey,
                                welcomeFlow = true,
                                encryptedPhraseData = base64UrlEncodedData,
                                importingPhrase = true
                            )

                            state =
                                state.copy(
                                    importPhraseState = ImportPhase.Completed,
                                    sendToSeedVerification = Resource.Success(route)
                                )
                        }

                        else -> {
                            //We will continue polling until completed so no need to respond to other states
                        }
                    }
                }
            }

            state = state.copy(getEncryptedResponse = response)
        }
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