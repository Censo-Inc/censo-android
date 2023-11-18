package co.censo.shared.service

import android.annotation.SuppressLint
import co.censo.shared.data.Resource
import co.censo.shared.data.networking.PushBody
import co.censo.shared.data.repository.PushRepository
import co.censo.shared.data.repository.PushRepositoryImpl.Companion.DEVICE_TYPE
import co.censo.shared.service.BaseMessagingService.Companion.BODY_KEY
import co.censo.shared.service.BaseMessagingService.Companion.DEFAULT_BODY
import co.censo.shared.service.BaseMessagingService.Companion.DEFAULT_TITLE
import co.censo.shared.service.BaseMessagingService.Companion.PUSH_TYPE_KEY
import co.censo.shared.service.BaseMessagingService.Companion.TITLE_KEY
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.sendError
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class BaseMessagingService : FirebaseMessagingService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    @Inject
    lateinit var pushRepository: PushRepository

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data.isNotEmpty()) {
            sendNotification(parsePushData(remoteMessage.data))
        }
    }

    /**
     * Called if the FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        scope.launch {
            sendRegistrationToServer(token)
        }
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    @SuppressLint("HardwareIds")
    fun sendRegistrationToServer(token: String?) {
        token?.let {
            scope.launch {
                val pushBody = PushBody(
                    deviceType = DEVICE_TYPE,
                    token = token
                )

                val pushResponse = pushRepository.addPushNotification(pushBody)

                if (pushResponse is Resource.Error) {
                    pushResponse.exception?.sendError(CrashReportingUtil.PushNotification)
                }
            }
        }
    }

    private fun parsePushData(data: Map<String, String>): PushData {
        return PushData(
            title = data.getOrDefault(TITLE_KEY, DEFAULT_TITLE),
            body = data.getOrDefault(BODY_KEY, DEFAULT_BODY),
            pushType = data.getOrDefault(PUSH_TYPE_KEY, ""),
        )
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    abstract fun sendNotification(pushData: PushData)

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    object Companion {
        const val TITLE_KEY = "title"
        const val BODY_KEY = "body"
        const val PUSH_TYPE_KEY = "pushType"

        const val DEFAULT_TITLE = "Censo Vault"
        const val DEFAULT_BODY = "Attention needed"
    }
}

data class PushData(
    val body: String,
    val title: String,
    val pushType: String,
)