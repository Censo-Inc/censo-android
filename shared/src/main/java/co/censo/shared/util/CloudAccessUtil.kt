package co.censo.shared.util

import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.presentation.cloud_storage.CloudAccessState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

suspend fun <T> observeCloudAccessStateForAccessGranted(
    keyRepository: KeyRepository,
    retryAction: suspend () -> T
): T {
    return withContext(currentCoroutineContext()) {
        val result = CompletableDeferred<T>()

        val job = launch(currentCoroutineContext()) {
            keyRepository.collectCloudAccessState { state ->
                if (state == CloudAccessState.AccessGranted) {
                    result.complete(retryAction())
                }
            }
        }

        result.await().also {
            job.cancel()
        }
    }
}
