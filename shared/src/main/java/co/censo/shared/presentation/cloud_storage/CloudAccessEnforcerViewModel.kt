package co.censo.shared.presentation.cloud_storage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.util.projectLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CloudAccessEnforcerViewModel @Inject constructor(
    private val keyRepository: KeyRepository,
) : ViewModel() {

    var state by mutableStateOf(CloudAccessEnforcerState())
        private set

    private var onAccessGranted: (() -> Unit)? = null

    fun onStart() {
//        projectLog(message = "onStart collecting cloud access state")
        viewModelScope.launch {
            keyRepository.collectCloudAccessState {
//                projectLog(message = "Cloud access state: $it")
                onCloudAccessState(it)
            }
        }
    }

    fun onAccessGranted() {
//        projectLog(message = "Access granted")
        keyRepository.updateCloudAccessState(CloudAccessState.AccessGranted)
    }

    private fun onCloudAccessState(cloudAccessState: CloudAccessState) {
        when (cloudAccessState) {
            CloudAccessState.Uninitialized -> {}
            is CloudAccessState.AccessRequired -> {
//                projectLog(message = "onCloudAccessState Access required")
//                projectLog(message = "onCloudAccessState Access required, setting local callback")
                onAccessGranted = cloudAccessState.onAccessGranted
//                projectLog(message = "onCloudAccessState Access required, setting enforce access")
                state = state.copy(enforceAccess = true)
            }
            CloudAccessState.AccessGranted -> {
//                projectLog(message = "onCloudAccessState Access granted")
                state = state.copy(enforceAccess = false)
//                projectLog(message = "onCloudAccessState Access granted, invoking local callback")
                onAccessGranted?.invoke()
//                projectLog(message = "onCloudAccessState Access granted, clearing local callback")
                onAccessGranted = null
            }
        }
    }

    fun onDispose() {
//        projectLog(message = "onDispose, CloudAccessEnforcer disposing")
        onAccessGranted = null
        state = CloudAccessEnforcerState()
    }
}