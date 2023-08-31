package co.censo.vault.data.repository

import co.censo.vault.data.Resource
import co.censo.vault.data.networking.ApiService
import okhttp3.ResponseBody

interface GuardianRepository {
    suspend fun registerGuardian(policyKey: String, participantId: String) : Resource<ResponseBody>
}

class GuardianRepositoryImpl(
    private val apiService: ApiService
) : GuardianRepository, BaseRepository() {
    override suspend fun registerGuardian(
        policyKey: String,
        participantId: String
    ): Resource<ResponseBody> {
        return retrieveApiResource { apiService.registerGuardian(policyKey, participantId) }
    }
}