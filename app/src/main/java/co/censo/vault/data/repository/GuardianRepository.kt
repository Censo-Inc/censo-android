package co.censo.vault.data.repository

import Base58EncodedPublicKey
import Base64EncodedData
import ParticipantId
import co.censo.vault.data.Resource
import co.censo.vault.data.cryptography.key.InternalDeviceKey
import co.censo.vault.data.model.AcceptGuardianshipApiRequest
import co.censo.vault.data.model.AcceptGuardianshipApiResponse
import co.censo.vault.data.model.GetGuardianStateApiResponse
import co.censo.vault.data.model.RegisterGuardianApiResponse
import co.censo.vault.data.model.Guardian
import co.censo.vault.data.networking.ApiService
import kotlinx.datetime.Clock
import okhttp3.ResponseBody
import java.util.Base64

interface GuardianRepository {
    suspend fun registerGuardian(intermediateKey: Base58EncodedPublicKey, participantId: ParticipantId) : Resource<RegisterGuardianApiResponse>
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
) : GuardianRepository, BaseRepository() {
    override suspend fun registerGuardian(
        intermediateKey: Base58EncodedPublicKey,
        participantId: ParticipantId
    ): Resource<RegisterGuardianApiResponse> {
        return retrieveApiResource { apiService.registerGuardian(intermediateKey.value, participantId.value) }
    }

    override fun signVerificationCode(verificationCode: String): Pair<Base64EncodedData, Long> {
        val currentTimeInMillis = Clock.System.now().toEpochMilliseconds()
        val dataToSign =
            Guardian.createNonceAndCodeData(time = currentTimeInMillis, code = verificationCode)
        val signature = InternalDeviceKey().sign(dataToSign)
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
}