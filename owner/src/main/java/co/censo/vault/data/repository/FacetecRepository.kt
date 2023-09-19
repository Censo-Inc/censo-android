package co.censo.vault.data.repository

import InitBiometryVerificationApiResponse
import co.censo.shared.data.Resource
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.data.model.SubmitBiometryVerificationApiRequest
import co.censo.shared.data.model.SubmitBiometryVerificationApiResponse
import co.censo.shared.data.networking.ApiService
import co.censo.shared.data.repository.BaseRepository

interface FacetecRepository {
    suspend fun startFacetecBiometry(): Resource<InitBiometryVerificationApiResponse>
    suspend fun submitResult(
        biometryId: BiometryVerificationId,
        biometryData: FacetecBiometry
    ): Resource<SubmitBiometryVerificationApiResponse>
}

class FacetecRepositoryImpl(private val apiService: ApiService) : FacetecRepository,
    BaseRepository() {
    override suspend fun startFacetecBiometry(): Resource<InitBiometryVerificationApiResponse> =
        retrieveApiResource { apiService.biometryVerification() }

    override suspend fun submitResult(
        biometryId: BiometryVerificationId,
        biometryData: FacetecBiometry
    ): Resource<SubmitBiometryVerificationApiResponse> {
        val facetecResultRequest = SubmitBiometryVerificationApiRequest(
            biometryData = biometryData
        )

        return retrieveApiResource {
            apiService.submitFacetecResult(
                biometryId = biometryId,
                facetecResultRequest = facetecResultRequest
            )
        }
    }
}