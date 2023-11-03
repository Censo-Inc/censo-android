package co.censo.vault.presentation.routing

import co.censo.shared.data.Resource
import co.censo.shared.data.model.GetUserApiResponse

data class OwnerRoutingState(
    val userResponse: Resource<GetUserApiResponse> = Resource.Loading(),

    val navigationResource: Resource<String> = Resource.Uninitialized,
)
