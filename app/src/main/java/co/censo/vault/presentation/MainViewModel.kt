package co.censo.vault.presentation

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    var state by mutableStateOf(MainState())
        private set

    fun onForeground(biometricCapability: BiometricUtil.Companion.BiometricsStatus) {
        viewModelScope.launch {
            state = state.copy(biometryStatus = biometricCapability)

            if (biometricCapability != BiometricUtil.Companion.BiometricsStatus.BIOMETRICS_ENABLED) {
                return@launch
            }

        }
    }

    fun blockUIStatus(): BlockAppUI {
        val biometryDisabled =
            state.biometryStatus != null && state.biometryStatus != BiometricUtil.Companion.BiometricsStatus.BIOMETRICS_ENABLED

        return when {
            biometryDisabled -> BlockAppUI.BIOMETRY_DISABLED
            else -> BlockAppUI.NONE
        }
    }
}