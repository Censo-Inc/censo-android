package co.censo.vault.presentation.main

import androidx.lifecycle.ViewModel
import co.censo.shared.data.networking.PushBody
import co.censo.shared.util.projectLog
import co.censo.vault.data.repository.PushRepository
import co.censo.vault.data.repository.PushRepositoryImpl.Companion.DEVICE_TYPE
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
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
            projectLog(message = "Exception caught while trying to submit notif token")
            //TODO: Log exception
        }
    }
}