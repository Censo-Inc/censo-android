package co.censo.vault.presentation.facetec_auth

import InitBiometryVerificationApiResponse
import co.censo.vault.data.Resource
import co.censo.vault.data.model.GetUserApiResponse

data class FacetecAuthState(
    val user: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val initFacetecData: Resource<InitBiometryVerificationApiResponse> = Resource.Uninitialized,
    val sessionId: String = "",
    val deviceKeyId: String = ""
)
