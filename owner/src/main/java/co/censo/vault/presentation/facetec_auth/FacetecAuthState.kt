package co.censo.vault.presentation.facetec_auth

import InitBiometryVerificationApiResponse
import co.censo.shared.data.Resource
import co.censo.shared.data.model.BiometryScanResultBlob

data class FacetecAuthState(
    //Async State
    val initFacetecData: Resource<InitBiometryVerificationApiResponse> = Resource.Uninitialized,
    val submitResultResponse: Resource<BiometryScanResultBlob> = Resource.Uninitialized,
    val startAuth: Resource<Unit> = Resource.Uninitialized,
    val userCancelled: Resource<Unit> = Resource.Uninitialized,

    //Standard State
    val facetecData : InitBiometryVerificationApiResponse? = null
) {
    val apiError = initFacetecData is Resource.Error
            || submitResultResponse is Resource.Error

    val loading = initFacetecData is Resource.Loading
            || submitResultResponse is Resource.Loading
            || startAuth is Resource.Success
}
