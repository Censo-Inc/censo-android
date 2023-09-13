package co.censo.vault.data.repository

import Base58EncodedDevicePublicKey
import Base58EncodedPublicKey
import Base64EncodedData
import ParticipantId
import co.censo.vault.data.Resource
import co.censo.vault.data.cryptography.CryptographyManager
import co.censo.vault.data.cryptography.ECIESManager
import co.censo.vault.data.cryptography.sha256
import co.censo.vault.data.model.AcceptGuardianshipApiRequest
import co.censo.vault.data.model.AcceptGuardianshipApiResponse
import co.censo.vault.data.model.GetGuardianStateApiResponse
import co.censo.vault.data.model.RegisterGuardianApiResponse
import co.censo.vault.data.networking.ApiService
import co.censo.vault.presentation.guardian_entrance.GuardianStatus
import io.github.novacrypto.base58.Base58
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.Base64

interface GuardianRepository {
    suspend fun registerGuardian(intermediateKey: Base58EncodedPublicKey, participantId: ParticipantId) : Resource<RegisterGuardianApiResponse>
    suspend fun encryptGuardianData(verificationCode: String, ownerDevicePublicKey: Base58EncodedDevicePublicKey) : Pair<ByteArray, AcceptGuardianData>
    fun signVerificationCode(verificationCode: String) : Pair<Base64EncodedData, Long>
    suspend fun getGuardian(
        intermediateKey: Base58EncodedPublicKey,
        participantId: ParticipantId
    ): Resource<GetGuardianStateApiResponse>
    suspend fun acceptGuardianship(
        intermediateKey: Base58EncodedPublicKey,
        participantId: ParticipantId,
        acceptGuardianshipApiRequest: AcceptGuardianshipApiRequest
    ): Resource<AcceptGuardianshipApiResponse>
    suspend fun declineGuardianship(
        intermediateKey: Base58EncodedPublicKey,
        participantId: ParticipantId,
    ): Resource<ResponseBody>
}

class GuardianRepositoryImpl(
    private val apiService: ApiService,
    private val cryptographyManager: CryptographyManager,
) : GuardianRepository, BaseRepository() {
    override suspend fun registerGuardian(
        intermediateKey: Base58EncodedPublicKey,
        participantId: ParticipantId
    ): Resource<RegisterGuardianApiResponse> {
        return retrieveApiResource { apiService.registerGuardian(intermediateKey.value, participantId.value) }
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

    override fun signVerificationCode(verificationCode: String): Pair<Base64EncodedData, Long> {
        val currentTimeInMillis = Clock.System.now().toEpochMilliseconds()
        val dataToSign = verificationCode.toByteArray() + currentTimeInMillis.toString().toByteArray()
        val signature = cryptographyManager.signData(dataToSign)
        val base64EncodedData = Base64EncodedData(Base64.getEncoder().encodeToString(signature))
        return Pair(base64EncodedData, currentTimeInMillis)
    }

    override suspend fun getGuardian(
        intermediateKey: Base58EncodedPublicKey,
        participantId: ParticipantId,
    ): Resource<GetGuardianStateApiResponse> {
        return retrieveApiResource { apiService.guardian(
            intermediateKey = intermediateKey.value,
            participantId = participantId.value,
        ) }
    }

    override suspend fun acceptGuardianship(
        intermediateKey: Base58EncodedPublicKey,
        participantId: ParticipantId,
        acceptGuardianshipApiRequest: AcceptGuardianshipApiRequest
    ): Resource<AcceptGuardianshipApiResponse> {
        return retrieveApiResource { apiService.acceptGuardianship(
            intermediateKey = intermediateKey.value,
            participantId = participantId.value,
            acceptGuardianshipApiRequest = acceptGuardianshipApiRequest
        ) }
    }

    override suspend fun declineGuardianship(
        intermediateKey: Base58EncodedPublicKey,
        participantId: ParticipantId
    ): Resource<ResponseBody> {
        return retrieveApiResource {
            apiService.declineGuardianship(
                intermediateKey = intermediateKey.value,
                participantId = participantId.value
            )
        }
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