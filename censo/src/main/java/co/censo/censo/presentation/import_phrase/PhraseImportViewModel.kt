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
import co.censo.shared.data.cryptography.key.EncryptionKey
import co.censo.shared.data.cryptography.key.ExternalEncryptionKey
import co.censo.shared.data.cryptography.sha256digest
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
import co.censo.shared.util.projectLog
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.novacrypto.base58.Base58
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.util.Base64
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class PhraseImportViewModel @Inject constructor(
    val keyRepository: KeyRepository,
    val ownerRepository: OwnerRepository,
    private val countDownTimer: VaultCountDownTimer
) : ViewModel() {

    var state by mutableStateOf(PhraseImportState())
        private set

    private fun startTimer(channel: String) {
        countDownTimer.start(2000) {
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
        val validSignature = verifySignature(importToAccept)

        if (!validSignature) {
            return
        }

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

    private fun verifySignature(import: Import): Boolean {
        val publicKey = recreateECPublicKey(import.importKey.getBytes())
        val key = ExternalEncryptionKey(publicKey)

        val nameAsUTF8 = String(Base64EncodedData(import.name).bytes, Charsets.UTF_8)

        var signedData = import.timestamp.toString().toByteArray(Charsets.UTF_8)
        signedData += nameAsUTF8.sha256digest()

        val derSignature = if (import.signature.bytes.size > 64) {
            import.signature.bytes
        } else {
            convertRawSignatureToDerFormat(import.signature.bytes)
        }

        val verified = key.verify(
            signedData = signedData,
            signature = derSignature
        )

        projectLog(message = "Signature verified: $verified")

        return if (verified) {
            val linkCreationTime = Instant.fromEpochMilliseconds(import.timestamp)
            val linkValidityStart = linkCreationTime.minus(10.seconds)
            val linkValidityEnd = linkCreationTime.plus(10.minutes)
            val now = Clock.System.now()

            if (now > linkValidityEnd) {
                projectLog(message = "Signature expired: $linkCreationTime")
                state = state.copy(importErrorType = ImportErrorType.LINK_EXPIRED)
                false
            } else if (now < linkValidityStart) {
                projectLog(message = "Signature in future: $linkCreationTime")
                state = state.copy(importErrorType = ImportErrorType.LINK_IN_FUTURE)
                false
            } else {
                projectLog(message = "Signature valid")
                true
            }
        } else {
            state = state.copy(importErrorType = ImportErrorType.BAD_SIGNATURE)
            false
        }
    }

    private fun recreateECPublicKey(rawBytes: ByteArray): PublicKey {
        val xAndYCoordinatesOnly = rawBytes.copyOfRange(1, rawBytes.size)
        val coordinateLength = xAndYCoordinatesOnly.size / 2
        val x = xAndYCoordinatesOnly.copyOfRange(0, coordinateLength)
        val y = xAndYCoordinatesOnly.copyOfRange(coordinateLength, xAndYCoordinatesOnly.size)

        val ecPoint = ECPoint(BigInteger(1, x), BigInteger(1, y))

        // Replace "secp256r1" with the name of the curve you are using
        val parameterSpec = ECNamedCurveTable.getParameterSpec("secp256r1")
        val params = ECNamedCurveSpec("secp256r1", parameterSpec.curve, parameterSpec.g, parameterSpec.n)

        val publicKeySpec = ECPublicKeySpec(ecPoint, params)

        val keyFactory = KeyFactory.getInstance("EC")
        return keyFactory.generatePublic(publicKeySpec)
    }

    private fun convertRawSignatureToDerFormat(signature: ByteArray) : ByteArray {
        fun isNegative(input: ByteArray) = input[0].toInt() < 0

        fun makePositive(input: ByteArray): ByteArray {
            return if (!isNegative(input)) {
                byteArrayOf(0x00) + input
            } else {
                input
            }
        }

        val r = makePositive(signature.copyOfRange(0, 32))
        val s = makePositive(signature.copyOfRange(32, 64))

        val derSignature = ByteBuffer.allocate(2 + r.size + 2 + s.size + 2).apply {
            put(0x30)
            put((r.size + s.size + 4).toByte())
            put(0x02)
            put(r.size.toByte())
            put(r)
            put(0x02)
            put(s.size.toByte())
            put(s)
        }

        return derSignature.array()
    }

    private fun checkForCompletedImport(channel: String) {
        viewModelScope.launch {
            val response = ownerRepository.checkForCompletedImport(channel)

            if (response is Resource.Success) {
                response.data?.let {
                    when (val importState = it.importState) {
                        is ImportState.Completed -> {
                            val masterPublicKey =
                                (state.userResponse.data as OwnerState.Ready).vault.publicMasterEncryptionKey

                            val route = Screen.EnterPhraseRoute.buildNavRoute(
                                masterPublicKey = masterPublicKey,
                                welcomeFlow = true,
                                encryptedPhraseData = importState.encryptedData.base64Encoded,
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