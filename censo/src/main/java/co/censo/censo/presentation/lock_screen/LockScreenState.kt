package co.censo.censo.presentation.lock_screen

import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.ProlongUnlockApiResponse
import co.censo.shared.data.model.UnlockApiResponse
import co.censo.shared.util.NavigationData
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class LockScreenState(
    val prolongUnlockResource: Resource<ProlongUnlockApiResponse> = Resource.Uninitialized,
    val lockStatus: LockStatus = LockStatus.None,
    val navigationResource: Resource<NavigationData> = Resource.Uninitialized
) {

    sealed class LockStatus {
        data object None : LockStatus()
        data class Locked(val canRequestBiometryReset: Boolean, val biometryResetRequested: Boolean = false) : LockStatus()
        data class Unlocked(val locksAt: Instant) : LockStatus()
        data class UnlockInProgress(val apiCall: Resource<UnlockApiResponse>) : LockStatus()

        companion object {
            fun fromOwnerState(ownerState: OwnerState): LockStatus =
                when (ownerState) {
                    is OwnerState.Ready -> fromOwnerStateReady(ownerState)
                    else -> None
                }

            private fun fromOwnerStateReady(ownerState: OwnerState.Ready): LockStatus {
                val locksAt = ownerState.locksAt
                return if (ownerState.authenticationReset != null) {
                    None    // allow to bypass lock screen while in the biometry reset mode
                } else if (locksAt == null || Clock.System.now() > locksAt) {
                    Locked(ownerState.canRequestAuthenticationReset)
                } else {
                    Unlocked(locksAt = locksAt)
                }
            }
        }
    }
}
