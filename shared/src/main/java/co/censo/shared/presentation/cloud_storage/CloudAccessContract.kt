package co.censo.shared.presentation.cloud_storage

interface CloudAccessContract {
    fun observeCloudAccessStateForAccessGranted(retryAction: () -> Unit)
}