package co.censo.vault.data.repository

import InitBiometryVerificationApiResponse
import co.censo.shared.data.Resource
import co.censo.shared.data.networking.ApiService
import co.censo.shared.data.repository.BaseRepository

interface FacetecRepository {
    suspend fun startFacetecBiometry(): Resource<InitBiometryVerificationApiResponse>
}

class FacetecRepositoryImpl(private val apiService: ApiService) : FacetecRepository, BaseRepository() {
    override suspend fun startFacetecBiometry(): Resource<InitBiometryVerificationApiResponse> =
        retrieveApiResource { apiService.initBiometryVerification() }
}