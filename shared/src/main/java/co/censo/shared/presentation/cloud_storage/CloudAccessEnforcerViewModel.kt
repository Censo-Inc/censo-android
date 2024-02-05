package co.censo.shared.presentation.cloud_storage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.data.repository.KeyRepository
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
        viewModelScope.launch {
            keyRepository.collectCloudAccessState {
                onCloudAccessState(it)
            }
        }
    }

    fun onAccessGranted() {
        keyRepository.updateCloudAccessState(CloudAccessState.AccessGranted)
    }

    private fun onCloudAccessState(cloudAccessState: CloudAccessState) {
        when (cloudAccessState) {
            CloudAccessState.Uninitialized -> {}
            is CloudAccessState.AccessRequired -> {
                onAccessGranted = cloudAccessState.onAccessGranted
                state = state.copy(enforceAccess = true)
            }
            CloudAccessState.AccessGranted -> {
                state = state.copy(enforceAccess = false)
                onAccessGranted?.invoke()
            }
        }
    }

    fun onDispose() {
        state = CloudAccessEnforcerState()
    }
}