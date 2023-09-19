package co.censo.vault.presentation.facetec_auth

import InitBiometryVerificationApiResponse
import co.censo.shared.data.Resource
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.SubmitBiometryVerificationApiResponse
import co.censo.shared.data.model.GetUserApiResponse

data class FacetecAuthState(
    //Async State
    //val userResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val initFacetecData: Resource<InitBiometryVerificationApiResponse> = Resource.Uninitialized,
    val submitResultResponse: Resource<BiometryScanResultBlob> = Resource.Uninitialized,
    val startAuth: Resource<Unit> = Resource.Uninitialized,

    //Standard State
    val facetecData : InitBiometryVerificationApiResponse? = null
) {
    val apiError = /*userResponse is Resource.Error
            ||*/ initFacetecData is Resource.Error
            || submitResultResponse is Resource.Error

    val loading = /*userResponse is Resource.Loading
            ||*/ initFacetecData is Resource.Loading
            || submitResultResponse is Resource.Loading
            || startAuth is Resource.Success
}
