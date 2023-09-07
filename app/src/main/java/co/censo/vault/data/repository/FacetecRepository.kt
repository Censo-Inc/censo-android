package co.censo.vault.data.repository

import InitBiometryVerificationApiResponse
import co.censo.vault.data.Resource
import co.censo.vault.data.model.SubmitBiometryVerificationApiRequest
import co.censo.vault.data.model.SubmitBiometryVerificationApiResponse
import co.censo.vault.data.networking.ApiService
import com.facetec.sdk.FaceTecSessionResult

interface FacetecRepository {
    suspend fun startFacetecBiometry(): Resource<InitBiometryVerificationApiResponse>
    suspend fun submitResult(
        biometryId: String,
        sessionResult: FaceTecSessionResult
    ): Resource<SubmitBiometryVerificationApiResponse>
}

class FacetecRepositoryImpl(private val apiService: ApiService) : FacetecRepository,
    BaseRepository() {
    override suspend fun startFacetecBiometry(): Resource<InitBiometryVerificationApiResponse> =
        retrieveApiResource { apiService.biometryVerification() }

    override suspend fun submitResult(
        biometryId: String,
        sessionResult: FaceTecSessionResult
    ): Resource<SubmitBiometryVerificationApiResponse> {
        val facetecResultRequest = SubmitBiometryVerificationApiRequest(
            faceScan = sessionResult.faceScanBase64,
            auditTrailImage = sessionResult.auditTrailCompressedBase64[0],
            lowQualityAuditTrailImage = sessionResult.lowQualityAuditTrailCompressedBase64[0]
        )

        return retrieveApiResource {
            apiService.submitFacetecResult(
                biometryId = biometryId,
                facetecResultRequest = facetecResultRequest
            )
        }
    }
}