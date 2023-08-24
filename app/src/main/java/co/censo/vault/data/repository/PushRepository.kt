package co.censo.vault.data.repository

import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import co.censo.vault.data.Resource
import co.censo.vault.data.networking.ApiService
import co.censo.vault.data.repository.PushRepositoryImpl.Companion.DEVICE_TYPE
import co.censo.vault.data.storage.SharedPrefsStorage
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import javax.inject.Inject

interface PushRepository {
    suspend fun addPushNotification(pushBody: PushBody): Resource<ResponseBody?>
    suspend fun removePushNotification()
    suspend fun retrievePushToken(): String
    fun userHasSeenPushDialog() : Boolean
    fun setUserSeenPushDialog(seenDialog: Boolean)
    suspend fun clearNotifications()
}

class PushRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val applicationContext: Context
) : PushRepository, BaseRepository() {

    private val notificationManager =
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val DEVICE_TYPE = "Android"
    }

    override suspend fun retrievePushToken(): String =
        FirebaseMessaging.getInstance().token.await()

    override fun userHasSeenPushDialog(): Boolean = SharedPrefsStorage.userHasSeenPermissionDialog()

    override fun setUserSeenPushDialog(seenDialog: Boolean) {
        SharedPrefsStorage.setUserSeenPermissionDialog(seenDialog)
    }

    override suspend fun addPushNotification(pushBody: PushBody): Resource<ResponseBody?> =
        retrieveApiResource { api.addPushNotificationToken(pushBody) }

    override suspend fun removePushNotification() {
        try {
            api.removePushNotificationToken(DEVICE_TYPE)
        } catch (e: Exception) {
            //TODO: Log the exception
        }
    }

    override suspend fun clearNotifications() {
        notificationManager.cancelAll()
    }
}

@Serializable
data class PushBody(
    val deviceType: String, val token: String
)