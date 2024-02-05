package co.censo.shared.presentation.cloud_storage

data class CloudAccessEnforcerState(
    val enforceAccess: Boolean = false,
)

enum class CloudAccessState {
    UNINITIALIZED, ACCESS_REQUIRED, ACCESS_GRANTED
}
