package co.censo.shared.util

import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.presentation.cloud_storage.CloudAccessState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

fun observeCloudAccessStateForAccessGranted(
    coroutineScope: CoroutineScope,
    keyRepository: KeyRepository,
    retryAction: () -> Unit
) {
    coroutineScope.launch {
        keyRepository.collectCloudAccessState {
            when (it) {
                CloudAccessState.AccessGranted -> {
                    retryAction()
                    //Stop collecting cloud access state
                    this.cancel()
                }
                else -> {}
            }
        }
    }
}