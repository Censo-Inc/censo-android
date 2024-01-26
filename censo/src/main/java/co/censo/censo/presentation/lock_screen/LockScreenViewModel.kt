package co.censo.censo.presentation.lock_screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.censo.presentation.Screen
import co.censo.shared.data.Resource
import co.censo.shared.data.model.BiometryScanResultBlob
import co.censo.shared.data.model.BiometryVerificationId
import co.censo.shared.data.model.FacetecBiometry
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.ProlongUnlockApiResponse
import co.censo.shared.data.model.UnlockApiResponse
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.VaultCountDownTimer
import co.censo.shared.util.asResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val prolongationThreshold = 10.minutes

@HiltViewModel
class LockScreenViewModel @Inject constructor(
    private val timer: VaultCountDownTimer,
    private val ownerRepository: OwnerRepository,
) : ViewModel() {

    var state by mutableStateOf(LockScreenState())
        private set

    fun onCreate() {
        viewModelScope.launch {
            ownerRepository.collectOwnerState { resource: Resource<OwnerState> ->
                onOwnerState(resource)
            }
        }
    }

    fun onStart() {
        retrieveOwnerState()

        timer.start(interval = 1.seconds.inWholeMilliseconds) {
            val lockStatus = state.lockStatus
            if (lockStatus is LockScreenState.LockStatus.Unlocked) {
                val now = Clock.System.now()
                if (now >= lockStatus.locksAt) {
                    resetToLocked()
                } else if (lockStatus.locksAt - now < prolongationThreshold) {
                    if (state.prolongUnlockResource !is Resource.Loading) {
                        prolongUnlock()
                    }
                }
            }
        }
    }

    fun onStop() {
        timer.stop()
    }

    private fun retrieveOwnerState() {
        viewModelScope.launch {
            val ownerStateResource = ownerRepository.retrieveUser().map { it.ownerState }

            if (ownerStateResource is Resource.Success) {
                ownerRepository.updateOwnerState(ownerStateResource)
            }
        }
    }

    fun resetToLocked() {
        state = state.copy(lockStatus = LockScreenState.LockStatus.Locked(canRequestBiometryReset = false))
        retrieveOwnerState()
    }

    private fun onOwnerState(ownerState: Resource<OwnerState>) {
        state = when (ownerState) {
            is Resource.Success -> state.copy(lockStatus = LockScreenState.LockStatus.fromOwnerState(ownerState.data))
            else -> state.copy(lockStatus = LockScreenState.LockStatus.None)
        }
    }

    fun initUnlock() {
        state = state.copy(
            lockStatus = LockScreenState.LockStatus.UnlockInProgress(
                apiCall = Resource.Uninitialized
            )
        )
    }

    suspend fun onFaceScanReady(
        verificationId: BiometryVerificationId,
        facetecData: FacetecBiometry
    ): Resource<BiometryScanResultBlob> {
        state = state.copy(
            lockStatus = LockScreenState.LockStatus.UnlockInProgress(
                apiCall = Resource.Loading
            )
        )

        return viewModelScope.async {
            val unlockVaultResponse: Resource<UnlockApiResponse> =
                ownerRepository.unlock(verificationId, facetecData)

            state = state.copy(
                lockStatus = LockScreenState.LockStatus.UnlockInProgress(
                    apiCall = unlockVaultResponse
                ),
            )

            if (unlockVaultResponse is Resource.Success) {
                ownerRepository.updateOwnerState(unlockVaultResponse.map { it.ownerState })
            }

            unlockVaultResponse.map { it.scanResultBlob }
        }.await()
    }

    private fun prolongUnlock() {
        state = state.copy(prolongUnlockResource = Resource.Loading)

        viewModelScope.launch {
            val response: Resource<ProlongUnlockApiResponse> = ownerRepository.prolongUnlock()

            if (response is Resource.Success) {
                ownerRepository.updateOwnerState(response.map { it.ownerState })
            }

            state = state.copy(prolongUnlockResource = response)
        }
    }

    fun navigateToResetBiometry(lockStatus: LockScreenState.LockStatus.Locked) {
        state = state.copy(
            // lock screen is placed on top of CensoNavHost. Need to keep loading indicator until
            // biometry reset is initiated after navigation is complete. Lock screen will be
            // released on incoming owner state update
            lockStatus = lockStatus.copy(biometryResetRequested = true),
            navigationResource = Screen.BiometryResetRoute.navToAndPopCurrentDestination().asResource()
        )
    }

    fun resetNavigationResource() {
        state = state.copy(navigationResource = Resource.Uninitialized)
    }
}
