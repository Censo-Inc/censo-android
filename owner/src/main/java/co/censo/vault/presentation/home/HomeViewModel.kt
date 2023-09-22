package co.censo.vault.presentation.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.repository.OwnerRepository
import co.censo.vault.data.repository.PushRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val pushRepository: PushRepository,
    private val ownerRepository: OwnerRepository
) : ViewModel() {

    var state by mutableStateOf(HomeState())
        private set

    fun onStart() {
        retrieveOwnerState()
    }

    fun retrieveOwnerState() {
        state = state.copy(ownerStateResource = Resource.Loading())
        viewModelScope.launch {
            val ownerStateResource = ownerRepository.retrieveUser().map { it.ownerState }
            state = state.copy(
                ownerStateResource = ownerStateResource,
            )
        }
    }

    fun updateOwnerState(ownerState: OwnerState) {
        state = state.copy(
            ownerStateResource = Resource.Success(ownerState),
        )
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