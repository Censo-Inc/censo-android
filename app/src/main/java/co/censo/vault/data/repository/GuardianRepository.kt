package co.censo.vault.data.repository

import Base58EncodedPublicKey
import ParticipantId
import co.censo.vault.data.Resource
import co.censo.vault.data.networking.ApiService
import okhttp3.ResponseBody

interface GuardianRepository {
    suspend fun registerGuardian(intermediateKey: Base58EncodedPublicKey, participantId: ParticipantId) : Resource<ResponseBody>
}

class GuardianRepositoryImpl(
    private val apiService: ApiService
) : GuardianRepository, BaseRepository() {
    override suspend fun registerGuardian(
        intermediateKey: Base58EncodedPublicKey,
        participantId: ParticipantId
    ): Resource<ResponseBody> {
        return retrieveApiResource { apiService.registerGuardian(intermediateKey, participantId) }
    }
}