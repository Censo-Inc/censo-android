package co.censo.vault.presentation.main

import androidx.lifecycle.ViewModel
import co.censo.shared.data.cryptography.key.InternalDeviceKey
import co.censo.shared.data.networking.PushBody
import co.censo.shared.data.storage.Storage
import co.censo.vault.data.repository.PushRepository
import co.censo.vault.data.repository.PushRepositoryImpl.Companion.DEVICE_TYPE
import co.censo.vault.util.vaultLog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val storage: Storage,
    private val deviceKey: InternalDeviceKey,
    private val pushRepository: PushRepository
) :
    ViewModel() {

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
}