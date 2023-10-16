package co.censo.vault.presentation.welcome

import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState

data class WelcomeState(
    val ownerStateResource: Resource<OwnerState> = Resource.Uninitialized,
) {
    val loading = ownerStateResource is Resource.Loading
    val asyncError = ownerStateResource is Resource.Error
}
