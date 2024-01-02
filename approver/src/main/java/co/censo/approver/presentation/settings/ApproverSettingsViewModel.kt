package co.censo.approver.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.approver.presentation.Screen
import co.censo.shared.data.Resource
import co.censo.shared.data.repository.ApproverRepository
import co.censo.shared.util.asResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApproverSettingsViewModel @Inject constructor(
    private val approverRepository: ApproverRepository
) : ViewModel() {

    var state by mutableStateOf(ApproverSettingsState())
        private set

    fun onStart() {
        retrieveApproverState(false)
    }

    fun retrieveApproverState(silently: Boolean) {
        if (!silently) {
            state = state.copy(userResponse = Resource.Loading())
        }

        viewModelScope.launch {
            val userResponse = approverRepository.retrieveUser()

            state = state.copy(userResponse = userResponse)
        }
    }

    fun setShowDeleteUserConfirmDialog() {
        state = state.copy(showDeleteUserConfirmDialog = true)
    }

    fun deleteUser() {
        state = state.copy(deleteUserResource = Resource.Loading())
        resetShowDeleteUserConfirmDialog()

        viewModelScope.launch {
            val deleteUserResource = approverRepository.deleteUser()

            state = state.copy(
                deleteUserResource = deleteUserResource
            )

            if (deleteUserResource is Resource.Success) {
                state = state.copy(navigationResource = Screen.ApproverEntranceRoute.navTo().asResource())
            }
        }
    }

    fun resetShowDeleteUserConfirmDialog() {
        state = state.copy(showDeleteUserConfirmDialog = false)
    }

    fun resetDeleteUserResource() {
        state = state.copy(deleteUserResource = Resource.Uninitialized)
    }

    fun navToOwnersListScreen() {
        state = state.copy(
            navigationResource = Screen.ApproverOwnersListScreen.navTo().asResource()
        )
    }

    fun resetNavigationResource() {
        state = state.copy(navigationResource = Resource.Uninitialized)
    }
}