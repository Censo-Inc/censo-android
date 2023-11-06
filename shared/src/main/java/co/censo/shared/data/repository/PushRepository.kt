package co.censo.shared.data.repository

import android.app.NotificationManager
import android.content.Context
import co.censo.shared.data.Resource
import co.censo.shared.data.networking.ApiService
import co.censo.shared.data.networking.PushBody
import co.censo.shared.data.storage.SecurePreferences
import co.censo.shared.util.sendError
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

interface PushRepository {
    suspend fun addPushNotification(pushBody: PushBody): Resource<Unit>
    suspend fun removePushNotification()
    suspend fun retrievePushToken(): String
    fun userHasSeenPushDialog() : Boolean
    fun setUserSeenPushDialog(seenDialog: Boolean)
    suspend fun clearNotifications()
}

class PushRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val secureStorage: SecurePreferences,
    applicationContext: Context
) : PushRepository, BaseRepository() {

    private val notificationManager =
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val DEVICE_TYPE = "Android"
    }

    override suspend fun retrievePushToken(): String =
        FirebaseMessaging.getInstance().token.await()

    override fun userHasSeenPushDialog(): Boolean = secureStorage.userHasSeenPermissionDialog()

    override fun setUserSeenPushDialog(seenDialog: Boolean) {
        secureStorage.setUserSeenPermissionDialog(seenDialog)
    }

    override suspend fun addPushNotification(pushBody: PushBody): Resource<Unit> =
        retrieveApiResource { api.addPushNotificationToken(pushBody) }

    override suspend fun removePushNotification() {
        try {
            api.removePushNotificationToken(DEVICE_TYPE)
        } catch (e: Exception) {
            e.sendError("RemovePushNotification")
        }
    }

    override suspend fun clearNotifications() {
        notificationManager.cancelAll()
    }
}
