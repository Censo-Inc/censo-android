package co.censo.vault.presentation.main

import co.censo.vault.util.BiometricUtil
import android.security.keystore.UserNotAuthenticatedException
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.cryptography.key.InternalDeviceKey
import co.censo.shared.data.cryptography.key.InternalDeviceKey.Companion.STATIC_DEVICE_KEY_CHECK
import co.censo.shared.data.networking.PushBody
import co.censo.vault.data.repository.PushRepository
import co.censo.vault.data.repository.PushRepositoryImpl.Companion.DEVICE_TYPE
import co.censo.shared.data.storage.Storage
import co.censo.vault.util.BioPromptReason
import co.censo.vault.util.vaultLog
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.security.ProviderException

@HiltViewModel
class MainViewModel @Inject constructor(
    private val storage: Storage,
    private val deviceKey: InternalDeviceKey,
    private val pushRepository: PushRepository
) :
    ViewModel() {
    var state by mutableStateOf(MainState())
        private set

    //Set this method in init block for vm to trigger push token registration for testing
    //TODO: Refactor to a better location once we have better understanding of the product flow
    fun triggerBiometryPromptForPushTokenRegistration() {
        state = state.copy(
            bioPromptTrigger = Resource.Success(Unit),
            bioPromptReason = BioPromptReason.PUSH_NOTIFICATION
        )
    }

    private suspend fun submitNotificationTokenForRegistration() {
        try {
            val token = pushRepository.retrievePushToken()
            if (token.isNotEmpty()) {
                val pushBody = PushBody(
                    deviceType = DEVICE_TYPE,
                    token = token
                )
//                pushRepository.addPushNotification(pushBody = pushBody)
            }
        } catch (e: Exception) {
            vaultLog(message = "Exception caught while trying to submit notif token")
            //TODO: Log exception
        }
    }


    fun updateAuthHeaders() {
        state = state.copy(
            bioPromptTrigger = Resource.Success(Unit),
            bioPromptReason = BioPromptReason.AUTH_HEADERS,
            blockAppUI = BlockAppUI.NONE
        )
    }

    fun onForeground(biometricCapability: BiometricUtil.Companion.BiometricsStatus) {
        state = state.copy(biometryStatus = biometricCapability)

        if (biometricCapability != BiometricUtil.Companion.BiometricsStatus.BIOMETRICS_ENABLED) {
            state = state.copy(blockAppUI = BlockAppUI.BIOMETRY_DISABLED)
            return
        }

        if (storage.storedPhrasesIsNotEmpty()) {
            launchBlockingForegroundBiometryRetrieval()
        }

    }

    fun launchBlockingForegroundBiometryRetrieval() {
        state = state.copy(
            bioPromptTrigger = Resource.Success(Unit),
            blockAppUI = BlockAppUI.FOREGROUND_BIOMETRY,
            bioPromptReason = BioPromptReason.FOREGROUND_RETRIEVAL
        )
    }

    fun onBiometryApproved() {
        when (state.bioPromptReason) {
            BioPromptReason.FOREGROUND_RETRIEVAL -> checkDataAfterBiometricApproval()
            BioPromptReason.AUTH_HEADERS -> signAuthHeaders()
            BioPromptReason.UNINITIALIZED -> {}
            BioPromptReason.PUSH_NOTIFICATION -> {
                state = state.copy(bioPromptTrigger = Resource.Uninitialized)
                viewModelScope.launch {
                    submitNotificationTokenForRegistration()
                }
            }
        }

        state = state.copy(bioPromptReason = BioPromptReason.UNINITIALIZED)
    }

    private fun signAuthHeaders() {
        val cachedReadCallHeaders = deviceKey.createAuthHeaders(Clock.System.now())
        storage.saveReadHeaders(cachedReadCallHeaders)

        state = state.copy(
            blockAppUI = BlockAppUI.NONE,
            bioPromptTrigger = Resource.Uninitialized,
            bioPromptReason = BioPromptReason.UNINITIALIZED
        )
    }

    private fun checkDataAfterBiometricApproval() {
        state =
            try {
                val encryptedData =
                    deviceKey.encrypt(STATIC_DEVICE_KEY_CHECK.toByteArray(Charsets.UTF_8))

                val decryptData =
                    String(deviceKey.decrypt(encryptedData))

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
        deviceKey.removeKey()
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