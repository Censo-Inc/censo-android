package co.censo.vault.presentation.facetec_auth

import InitBiometryVerificationApiResponse
import co.censo.vault.data.Resource
import co.censo.vault.data.model.FacetecResultResponse
import co.censo.vault.data.model.GetUserApiResponse

data class FacetecAuthState(
    //Async State
    val userResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val initFacetecData: Resource<InitBiometryVerificationApiResponse> = Resource.Uninitialized,
    val submitResultResponse: Resource<FacetecResultResponse> = Resource.Uninitialized,
    val startAuth: Resource<Unit> = Resource.Uninitialized,

    //Standard State
    val biometryId: String = "",
    val sessionToken: String = "",
    val deviceKeyId: String = "",
    val biometryEncryptionPublicKey: String = "",
) {
    val apiError = userResponse is Resource.Error
            || initFacetecData is Resource.Error
            || submitResultResponse is Resource.Error

    val loading = userResponse is Resource.Loading
            || initFacetecData is Resource.Loading
            || submitResultResponse is Resource.Loading
            || startAuth is Resource.Success
}
