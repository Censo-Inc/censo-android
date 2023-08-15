package co.censo.vault.presentation.main

import BiometricUtil
import android.os.Build
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.UserNotAuthenticatedException
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import co.censo.vault.CryptographyManager
import co.censo.vault.CryptographyManagerImpl.Companion.STATIC_DEVICE_KEY_CHECK
import co.censo.vault.Resource
import co.censo.vault.storage.SharedPrefsHelper
import java.security.InvalidAlgorithmParameterException
import java.security.ProviderException
import javax.crypto.Cipher

@HiltViewModel
class MainViewModel @Inject constructor(private val cryptographyManager: CryptographyManager) :
    ViewModel() {
    var state by mutableStateOf(MainState())
        private set

    fun onForeground(biometricCapability: BiometricUtil.Companion.BiometricsStatus) {
        viewModelScope.launch {
            state = state.copy(biometryStatus = biometricCapability)

            if (biometricCapability != BiometricUtil.Companion.BiometricsStatus.BIOMETRICS_ENABLED) {
                return@launch
            }

            if (cryptographyManager.deviceKeyExists() && SharedPrefsHelper.storedPhrasesIsNotEmpty()) {
                launchBlockingForegroundBiometryRetrieval()
            } else {
                cryptographyManager.createDeviceKeyIfNotExists()
            }
        }
    }

    fun launchBlockingForegroundBiometryRetrieval() {
        state = state.copy(
            bioPromptTrigger = Resource.Success(Unit),
        )
    }

    fun onBiometryApproved() {
        checkDataAfterBiometricApproval()
    }

    private fun checkDataAfterBiometricApproval() {
        state =
            try {
                val encryptedData = cryptographyManager.encryptData(STATIC_DEVICE_KEY_CHECK)

                val decryptData =
                    String(cryptographyManager.decryptData(encryptedData))

                if (decryptData == STATIC_DEVICE_KEY_CHECK) {
                    biometrySuccessfulState()
                } else {
                    state.copy(bioPromptTrigger = Resource.Error())
                }
            } catch (e: Exception) {
                if (keyRanOutOfTime(e)) {
                    clearOutSavedData()
                }
                state.copy(bioPromptTrigger = Resource.Error())
            }
    }

    private fun keyRanOutOfTime(exception: Exception) =
        when {
            exception is UserNotAuthenticatedException -> true
            exception is ProviderException && exception.cause is android.security.KeyStoreException -> true
            else -> false
        }

    fun clearOutSavedData() {
        cryptographyManager.deleteDeviceKeyIfPresent()
        SharedPrefsHelper.clearStoredPhrases()
    }

    private fun biometrySuccessfulState(): MainState =
        state.copy(bioPromptTrigger = Resource.Uninitialized)

    fun onBiometryFailed(errorCode: Int) {
        state =
            if (BiometricUtil.getBioPromptFailedReason(errorCode) == BiometricUtil.BioPromptFailedReason.FAILED_TOO_MANY_ATTEMPTS) {
                state.copy(
                    tooManyAttempts = true,
                    bioPromptTrigger = Resource.Error()
                )
            } else {
                state.copy(bioPromptTrigger = Resource.Error())
            }
    }

    fun blockUIStatus(): BlockAppUI {
        val visibleBlockingUi = state.bioPromptTrigger !is Resource.Uninitialized
        val biometryDisabled =
            state.biometryStatus != null && state.biometryStatus != BiometricUtil.Companion.BiometricsStatus.BIOMETRICS_ENABLED

        return when {
            biometryDisabled -> BlockAppUI.BIOMETRY_DISABLED
            visibleBlockingUi -> BlockAppUI.FOREGROUND_BIOMETRY
            else -> BlockAppUI.NONE
        }
    }
}