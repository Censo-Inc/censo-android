package co.censo.vault.presentation.owner_entrance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.repository.KeyRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.vault.presentation.home.HomeScreen
import co.censo.vault.presentation.home.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 *
 * General Android Owner Flow
 * Step 1: Login user with Primary Auth (OneTap)
 *       - If login error occurs notify user
 * Step 2: Handle Device Key Work: TODO
 * Step 3: Send user to Home screen
 *
 */


@HiltViewModel
class OwnerEntranceViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val keyRepository: KeyRepository
) : ViewModel() {

    var state by mutableStateOf(OwnerEntranceState())
        private set

    fun onStart() {
        checkUserHasValidToken()
    }

    private fun checkUserHasValidToken() {
        viewModelScope.launch {
            val jwtToken = ownerRepository.retrieveJWT()
            if (jwtToken.isNotEmpty()) {
                val tokenValid = ownerRepository.checkJWTValid(jwtToken)

                if (tokenValid) {
                    state = state.copy(
                        userFinishedSetup = Resource.Success(Screen.HomeRoute.route)
                    )
                }
            }
        }
    }

    fun startOneTapFlow() {
        state = state.copy(triggerOneTap = Resource.Success(Unit))
    }
    fun oneTapSuccess(googleIdCredential: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val idToken = try {
                ownerRepository.verifyToken(googleIdCredential)
            } catch (e: Exception) {
                oneTapFailure(OneTapError.FailedToVerifyId(e))
                return@launch
            }

            if (idToken == null) {
                oneTapFailure(OneTapError.InvalidToken)
                return@launch
            }

            if (!keyRepository.hasKeyWithId(idToken)) {
                keyRepository.createAndSaveKeyWithId(idToken)
            } else {
                keyRepository.setSavedDeviceId(idToken)
            }

            ownerRepository.saveJWT(googleIdCredential)

            val createUserResponse = ownerRepository.createUser(
                jwtToken = googleIdCredential,
                idToken = idToken
            )

            state = if (createUserResponse is Resource.Success) {
                state.copy(
                    userFinishedSetup = Resource.Success(Screen.HomeRoute.route),
                    createUserResource = createUserResponse
                )
            } else {
                state.copy(createUserResource = createUserResponse)
            }
        }
    }
    fun oneTapFailure(oneTapError: OneTapError) {
        state = state.copy(triggerOneTap = Resource.Error(exception = oneTapError.exception))
    }

    fun retryCreateUser() {
        startOneTapFlow()
    }

    fun resetTriggerOneTap() {
        state = state.copy(triggerOneTap = Resource.Uninitialized)
    }

    fun resetUserFinishedSetup() {
        state = state.copy(userFinishedSetup = Resource.Uninitialized)
    }

    fun resetCreateOwnerResource() {
        state = state.copy(createUserResource = Resource.Uninitialized)
    }
}