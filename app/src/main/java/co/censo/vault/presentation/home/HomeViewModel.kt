package co.censo.vault.presentation.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import co.censo.vault.data.Resource
import co.censo.vault.data.repository.PushRepository
import co.censo.vault.data.storage.Storage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val storage: Storage,
    private val pushRepository: PushRepository
) : ViewModel() {

    var state by mutableStateOf(HomeState())
        private set

    fun onStart() {
        state = state.copy(phrases = storage.retrieveBIP39Phrases())
    }

    fun userHasSeenPushDialog() = pushRepository.userHasSeenPushDialog()

    fun setUserSeenPushDialog(seenDialog: Boolean) =
        pushRepository.setUserSeenPushDialog(seenDialog)

    fun triggerPushNotificationDialog() {
        state = state.copy(showPushNotificationsDialog = Resource.Success(Unit))
    }

    fun resetPushNotificationDialog() {
        state = state.copy(showPushNotificationsDialog = Resource.Uninitialized)
    }
}