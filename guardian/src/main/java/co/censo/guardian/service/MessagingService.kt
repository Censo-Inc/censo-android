package co.censo.guardian.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.net.toUri
import co.censo.guardian.MainActivity
import co.censo.guardian.R
import co.censo.shared.SharedScreen
import co.censo.shared.buildScreenDeepLinkUri
import co.censo.shared.service.BaseMessagingService
import co.censo.shared.service.PushData
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import kotlin.math.abs

@AndroidEntryPoint
class MessagingService : BaseMessagingService() {

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    override fun sendNotification(pushData: PushData) {
        val homeScreenIntent = Intent(
            Intent.ACTION_VIEW,
            SharedScreen.HomeRoute.buildScreenDeepLinkUri().toUri(),
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
}