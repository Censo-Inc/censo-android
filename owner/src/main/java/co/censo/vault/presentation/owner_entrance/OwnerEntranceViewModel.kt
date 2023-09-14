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
 * Step 1: Check users saved timestamp
 *       - Do biometry to sign timestamp if needed
 * Step 2: Check User State: GET /user
 *      - If no user, then will create
 *      - If need to complete Facetec action, verify user w/ Facetec
 * Step 3: Send user to list of BIP 39 phrases
 *
 * First Time Android Owner Flow:
 * Step 1: Check users saved timestamp
 *      - Do biometry to sign timestamp if needed
 * Step 2: Check User State: GET /user
 * Step 3: Receive 401
 *      - POST /user
 * Step 5: Check User State: GET /user
 * Step 6: Send user to Facetec enrollment/auth
 *      - *Not implemented yet*
 * Step 7: Owner is fully created
 *
 */

@HiltViewModel
class OwnerEntranceViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
) : ViewModel() {

    var state by mutableStateOf(OwnerEntranceState())
        private set

    fun onStart() {
        if (ownerRepository.checkValidTimestamp()) {
            vaultLog(message = "Have a valid timestamp moving forward...")
            checkUserState()
        } else {
            vaultLog(message = "Timestamp missing or expired, triggering biometry...")
            triggerBiometryPrompt()
        }
    }

    private fun triggerBiometryPrompt() {
        state = state.copy(bioPromptTrigger = Resource.Success(Unit))
    }

    fun onBiometryApproved() {
        ownerRepository.saveValidTimestamp()

        checkUserState()
        state = state.copy(bioPromptTrigger = Resource.Uninitialized)
    }

    fun onBiometryFailed() {
        state = state.copy(bioPromptTrigger = Resource.Uninitialized)
    }

    private suspend fun registerUserToBackend() {
        val createOwnerResponse = ownerRepository.createOwner()

        if (createOwnerResponse is Resource.Success) {
            state = state.copy(
                userStatus = UserStatus.UNINITIALIZED,
                createOwnerResource = createOwnerResponse
            )
        } else if (createOwnerResponse is Resource.Error) {
            state = state.copy(createOwnerResource = createOwnerResponse)
        }
    }

    private fun checkUserState() {
        state = state.copy(userResource = Resource.Loading())
        viewModelScope.launch {
            val user = ownerRepository.retrieveUser()
            vaultLog(message = "User coming back from retrieve user: ${user.data}")
            when (user) {
                is Resource.Error -> {
                    vaultLog(message = "Retrieve user failed")
                    state =
                        if (user.errorCode != null && user.errorCode == HTTP_404) {
                            vaultLog(message = "Received 404. User not created.")
                            state.copy(
                                userResource = Resource.Uninitialized
                            )
                        } else {
                            vaultLog(message = "Failed to retrieve user")
                            state.copy(userResource = user)
                        }
                }

                is Resource.Success -> {
                    vaultLog(message = "Retrieve user success")
                    if (user.data == null) {
                        state =
                            state.copy(userStatus = UserStatus.UNINITIALIZED, userResource = user)
                        return@launch
                    }

                    val nextScreen =
                        if (user.data!!.biometricVerificationRequired) Screen.FacetecAuthRoute.route else Screen.HomeRoute.route

                    state =
                        state.copy(
                            userStatus = UserStatus.UNINITIALIZED,
                            userFinishedSetup = Resource.Success(nextScreen),
                            userResource = user
                        )
                    return@launch
                }

                else -> {
                    state = state.copy(userResource = user)
                }
            }
        }
    }

    fun retryCreateUser() {
        viewModelScope.launch {
            registerUserToBackend()
        }
    }

    fun retryGetUser() {
        viewModelScope.launch {
            checkUserState()
        }
    }

    fun resetUserFinishedSetup() {
        state = state.copy(userFinishedSetup = Resource.Uninitialized)
    }

    fun resetUserResource() {
        state = state.copy(userResource = Resource.Uninitialized)
    }

    fun resetCreateOwnerResource() {
        state = state.copy(createOwnerResource = Resource.Uninitialized)
    }
}