package co.censo.censo.presentation.push_notification

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.networking.PushBody
import co.censo.shared.data.repository.PushRepository
import co.censo.shared.data.repository.PushRepositoryImpl
import co.censo.shared.util.CrashReportingUtil
import co.censo.shared.util.sendError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PushNotificationViewModel @Inject constructor(
    private val pushRepository: PushRepository,
    ) : ViewModel() {

    var state by mutableStateOf(PushNotificationState())
        private set


    fun userHasSeenPushDialog() = pushRepository.userHasSeenPushDialog()

    fun setUserSeenPushDialog(seenDialog: Boolean) =
        pushRepository.setUserSeenPushDialog(seenDialog)

    fun checkUserHasRespondedToNotificationOptIn() {
        viewModelScope.launch {
            state = if (pushRepository.userHasSeenPushDialog()) {
                submitNotificationTokenForRegistration()
                state.copy(userResponded = Resource.Success(Unit))
            } else {
                state.copy(
                    showPushNotificationsDialog = Resource.Success(Unit)
                )
            }
        }
    }

    fun resetUserResponded() {
        state = state.copy(userResponded = Resource.Uninitialized)
    }

    fun setUserResponded() {
        state = state.copy(userResponded = Resource.Success(Unit))
    }

    private fun submitNotificationTokenForRegistration() {
        viewModelScope.launch {
            try {
                val token = pushRepository.retrievePushToken()
                if (token.isNotEmpty()) {
                    val pushBody = PushBody(
                        deviceType = PushRepositoryImpl.DEVICE_TYPE,
                        token = token
                    )
                    pushRepository.addPushNotification(pushBody = pushBody)
                }
            } catch (e: Exception) {
                e.sendError(CrashReportingUtil.SubmitNotificationToken)
            }
            state = state.copy(userResponded = Resource.Success(Unit))
        }
    }

    fun finishPushNotificationDialog() {
        submitNotificationTokenForRegistration()
        state = state.copy(showPushNotificationsDialog = Resource.Uninitialized)
    }
}