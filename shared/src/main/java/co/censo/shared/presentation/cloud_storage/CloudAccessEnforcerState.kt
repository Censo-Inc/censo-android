package co.censo.shared.presentation.cloud_storage

data class CloudAccessEnforcerState(
    val enforceAccess: Boolean = false,
)

sealed class CloudAccessState {
    data object Uninitialized : CloudAccessState()
    data object AccessRequired : CloudAccessState()
    data object AccessGranted : CloudAccessState()
}