package co.censo.vault.presentation.home

import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState

data class HomeState(
    val showPushNotificationsDialog: Resource<Unit> = Resource.Uninitialized,
    val ownerStateResource: Resource<OwnerState?> = Resource.Uninitialized,
) {
    val loading = ownerStateResource is Resource.Loading
    val asyncError = ownerStateResource is Resource.Error
}
