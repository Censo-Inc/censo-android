package co.censo.vault.presentation.facetec_auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.vault.data.Resource
import co.censo.vault.data.repository.FacetecRepository
import co.censo.vault.util.vaultLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FacetecAuthViewModel @Inject constructor(
    private val facetecRepository: FacetecRepository
) : ViewModel() {

    var state by mutableStateOf(FacetecAuthState())
        private set

    fun onStart() {
        retrieveFacetecData()
    }

    private fun retrieveFacetecData() {
        viewModelScope.launch {
            val facetecDataResource = facetecRepository.startFacetecBiometry()

            if (facetecDataResource is Resource.Success) {
                state = state.copy(
                    initFacetecData = facetecDataResource,
                    sessionId = facetecDataResource.data?.sessionToken ?: "",
                    deviceKeyId = facetecDataResource.data?.deviceKeyId ?: ""
                )
            }
        }
    }

    fun facetecSDKInitialized() {
        vaultLog(message = "Successfully set up Facetec SDK")
    }

    fun failedToInitializeSDK() {
        vaultLog(message = "Failed to setup Facetec SDK")
    }

    fun resetFacetecInitDataResource() {
        state = state.copy(initFacetecData = Resource.Uninitialized)
    }
}