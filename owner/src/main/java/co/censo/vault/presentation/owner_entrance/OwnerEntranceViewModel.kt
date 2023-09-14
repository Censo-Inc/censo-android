package co.censo.vault.presentation.owner_entrance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.repository.BaseRepository.Companion.HTTP_404
import co.censo.shared.data.repository.OwnerRepository
import co.censo.vault.presentation.home.Screen
import co.censo.vault.util.vaultLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 *
 * General Android Owner Flow
 * Step 1: Login user with Primary Auth (OneTap)
 *       - If login error occurs notify user
 * Step 2: Register user with auth id returned from successful Primary Auth login
 *      - If user is not created on the backend, they will be
 *      - Otherwise user has been logged in/registered with backend
 * Step 3: Send user to Home screen
 *
 */

@HiltViewModel
class OwnerEntranceViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
) : ViewModel() {

    var state by mutableStateOf(OwnerEntranceState())
        private set

    fun registerUserToBackend(authId: String) {
        if (authId.isNotEmpty() && state.authId.isEmpty()) {
            state = state.copy(authId = authId)
        }

        viewModelScope.launch {
            val createOwnerResponse = ownerRepository.createOwner(state.authId)

            if (createOwnerResponse is Resource.Success) {
                state = state.copy(userFinishedSetup = Resource.Success(Screen.HomeRoute.route))
            } else if (createOwnerResponse is Resource.Error) {
                state = state.copy(createOwnerResource = createOwnerResponse)
            }
        }
    }

    fun retryCreateUser() {
        viewModelScope.launch {
            registerUserToBackend(state.authId)
        }
    }

    fun resetUserFinishedSetup() {
        state = state.copy(userFinishedSetup = Resource.Uninitialized)
    }

    fun resetCreateOwnerResource() {
        state = state.copy(createOwnerResource = Resource.Uninitialized)
    }
}