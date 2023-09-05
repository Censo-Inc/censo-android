package co.censo.vault.data.repository

import InitBiometryVerificationApiResponse
import co.censo.vault.data.Resource
import co.censo.vault.data.networking.ApiService

interface FacetecRepository {
    suspend fun startFacetecBiometry(): Resource<InitBiometryVerificationApiResponse>
}

class FacetecRepositoryImpl(private val apiService: ApiService) : FacetecRepository,
    BaseRepository() {
    override suspend fun startFacetecBiometry(): Resource<InitBiometryVerificationApiResponse> {
        //return retrieveApiResource { apiService.biometryVerification() }

        return Resource.Success(
            data = InitBiometryVerificationApiResponse(
                sessionToken = "",
                deviceKeyId = "djTgh5PezWWqKfjKmEFHb4PnYtB8FaO6",
                firstTime = true,
                biometryEncryptionPublicKey = ""
            )
        )
    }
}