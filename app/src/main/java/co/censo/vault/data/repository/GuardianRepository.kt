package co.censo.vault.data.repository

import Base58EncodedDevicePublicKey
import Base58EncodedPublicKey
import ParticipantId
import co.censo.vault.data.Resource
import co.censo.vault.data.cryptography.CryptographyManager
import co.censo.vault.data.cryptography.ECIESManager
import co.censo.vault.data.cryptography.sha256
import co.censo.vault.data.networking.ApiService
import io.github.novacrypto.base58.Base58
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody

interface GuardianRepository {
    suspend fun registerGuardian(intermediateKey: Base58EncodedPublicKey, participantId: ParticipantId) : Resource<ResponseBody>
    suspend fun encryptGuardianData(verificationCode: String, ownerDevicePublicKey: Base58EncodedDevicePublicKey) : Pair<ByteArray, AcceptGuardianData>
}

class GuardianRepositoryImpl(
    private val apiService: ApiService,
    private val cryptographyManager: CryptographyManager
) : GuardianRepository, BaseRepository() {
    override suspend fun registerGuardian(
        intermediateKey: Base58EncodedPublicKey,
        participantId: ParticipantId
    ): Resource<ResponseBody> {
        return retrieveApiResource { apiService.registerGuardian(intermediateKey, participantId) }
    }

    override suspend fun encryptGuardianData(verificationCode: String, ownerDevicePublicKey: Base58EncodedDevicePublicKey) : Pair<ByteArray, AcceptGuardianData> {
        val nonce = generateNonce()

        val hashedCodeAndNonce = "$verificationCode${nonce}".sha256()

        val encryptedCodeAndNonce = ECIESManager.encryptMessage(
            hashedCodeAndNonce.toByteArray(Charsets.UTF_8),
            Base58.base58Decode(ownerDevicePublicKey.value)
        )

        val guardianDevicePublicKey = cryptographyManager.getPublicKeyFromDeviceKey()

        val uncompressedGuardianDevicePublicKey =
            ECIESManager.extractUncompressedPublicKey(guardianDevicePublicKey.encoded)

        val guardianData = AcceptGuardianData(
            guardianPublicDeviceKey = Base58.base58Encode(uncompressedGuardianDevicePublicKey),
            encryptedCodeAndNonce = Base58.base58Encode(encryptedCodeAndNonce),
            nonce = nonce
        )

        val json = Json.encodeToString(guardianData)

        val signedData = cryptographyManager.signData(json.toByteArray(Charsets.UTF_8))

        return Pair(signedData, guardianData)
    }

    fun generateNonce() = Clock.System.now().toEpochMilliseconds().toString()
}

typealias Base58EncryptedCodeAndNonce = String

@Serializable
data class AcceptGuardianData(
    val guardianPublicDeviceKey: String,
    val encryptedCodeAndNonce: Base58EncryptedCodeAndNonce,
    val nonce: String
)