package co.censo.censo.presentation.owner_key_validation

import co.censo.shared.data.Resource

data class OwnerKeyValidationState(
    val ownerKeyUIState: OwnerKeyValidationUIState = OwnerKeyValidationUIState.None,
    val navigationResource: Resource<String> = Resource.Uninitialized
) {
    enum class OwnerKeyValidationUIState {
        None, FileNotFound
    }
}