package co.censo.censo.presentation.push_notification

import co.censo.shared.data.Resource

data class PushNotificationState(
    val showPushNotificationsDialog: Resource<Unit> = Resource.Uninitialized,
    val userResponded: Resource<Unit> = Resource.Uninitialized
)
