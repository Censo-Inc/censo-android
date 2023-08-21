package co.censo.vault.presentation.main

import co.censo.vault.util.BiometricUtil
import android.security.keystore.UserNotAuthenticatedException
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import co.censo.vault.data.cryptography.CryptographyManager
import co.censo.vault.data.cryptography.CryptographyManagerImpl.Companion.STATIC_DEVICE_KEY_CHECK
import co.censo.vault.data.Resource
import co.censo.vault.data.storage.Storage
import java.security.ProviderException

@HiltViewModel
class MainViewModel @Inject constructor(
    private val cryptographyManager: CryptographyManager,
    private val storage: Storage
) :
    ViewModel() {
    var state by mutableStateOf(MainState())
        private set

    fun onForeground(biometricCapability: BiometricUtil.Companion.BiometricsStatus) {
        state = state.copy(biometryStatus = biometricCapability)

        if (biometricCapability != BiometricUtil.Companion.BiometricsStatus.BIOMETRICS_ENABLED) {
            state = state.copy(blockAppUI = BlockAppUI.BIOMETRY_DISABLED)
            return
        }

        if (cryptographyManager.deviceKeyExists() && storage.storedPhrasesIsNotEmpty()) {
            launchBlockingForegroundBiometryRetrieval()
        } else {
            cryptographyManager.createDeviceKeyIfNotExists()
        }

    }

    fun launchBlockingForegroundBiometryRetrieval() {
        state = state.copy(
            bioPromptTrigger = Resource.Success(Unit),
            blockAppUI = BlockAppUI.FOREGROUND_BIOMETRY
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
                } else {
                    state.copy(bioPromptTrigger = Resource.Error())
                }
            }
    }

    private fun keyRanOutOfTime(exception: Exception) =
        when {
            exception is UserNotAuthenticatedException -> true
            exception is ProviderException && exception.cause is android.security.KeyStoreException -> true
            else -> false
        }

    private fun clearOutSavedData() : MainState {
        cryptographyManager.deleteDeviceKeyIfPresent()
        storage.clearStoredPhrases()
         return state.copy(
            blockAppUI = BlockAppUI.NONE,
            biometryInvalidated = Resource.Success(Unit),
            bioPromptTrigger = Resource.Uninitialized
        )
    }

    private fun biometrySuccessfulState(): MainState =
        state.copy(
            bioPromptTrigger = Resource.Uninitialized,
            blockAppUI = BlockAppUI.NONE
        )

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

    fun resetBiometryInvalidated() {
        state = state.copy(biometryInvalidated = Resource.Uninitialized)
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