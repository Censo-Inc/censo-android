package co.censo.censo.presentation.owner_key_validation

import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState

data class OwnerKeyValidationState(
    val ownerKeyUIState: OwnerKeyValidationUIState = OwnerKeyValidationUIState.None,
    val navigationResource: Resource<String> = Resource.Uninitialized,
    val triggerDeleteUserDialog: Resource<Unit> = Resource.Uninitialized,
    val ownerState: OwnerState.Ready? = null
) {
    enum class OwnerKeyValidationUIState {
        None, FileNotFound
    }
}