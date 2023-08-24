package co.censo.vault.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.net.toUri
import co.censo.vault.MainActivity
import co.censo.vault.R
import co.censo.vault.data.Resource
import co.censo.vault.data.repository.PushBody
import co.censo.vault.data.repository.PushRepository
import co.censo.vault.data.repository.PushRepositoryImpl.Companion.DEVICE_TYPE
import co.censo.vault.presentation.home.Screen
import co.censo.vault.service.MessagingService.Companion.BODY_KEY
import co.censo.vault.service.MessagingService.Companion.DEFAULT_BODY
import co.censo.vault.service.MessagingService.Companion.DEFAULT_TITLE
import co.censo.vault.service.MessagingService.Companion.PUSH_TYPE_KEY
import co.censo.vault.service.MessagingService.Companion.TITLE_KEY
import co.censo.vault.util.vaultLog
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class MessagingService : FirebaseMessagingService() {

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
                    vaultLog(message = "Push notification registration failed")
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
    private fun sendNotification(pushData: PushData) {
        val homeScreenIntent = Intent(
            Intent.ACTION_VIEW,
            Screen.HomeRoute.buildScreenDeepLinkUri().toUri(),
            this,
            MainActivity::class.java
        )

        homeScreenIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(homeScreenIntent)
            getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE)
        }

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(pushData.title)
            .setContentText(pushData.body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            getString(R.string.default_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val notificationId = abs(Date().time.toInt())
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

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