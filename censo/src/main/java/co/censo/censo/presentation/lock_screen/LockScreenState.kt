package co.censo.censo.presentation.lock_screen

import co.censo.shared.data.Resource
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.ProlongUnlockApiResponse
import co.censo.shared.data.model.UnlockApiResponse
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class LockScreenState(
    val prolongUnlockResource: Resource<ProlongUnlockApiResponse> = Resource.Uninitialized,
    val lockStatus: LockStatus = LockStatus.None
) {

    sealed class LockStatus {
        object None : LockStatus()
        object Locked : LockStatus()
        data class Unlocked(val locksAt: Instant) : LockStatus()
        data class UnlockInProgress(val apiCall: Resource<UnlockApiResponse>) : LockStatus()

        companion object {
            fun fromOwnerState(ownerState: OwnerState): LockStatus =
                when (ownerState) {
                    is OwnerState.Ready -> fromLocksAt(ownerState.locksAt)
                    else -> None
                }

            private fun fromLocksAt(locksAt: Instant?): LockStatus {
                return if (locksAt == null || Clock.System.now() > locksAt) {
                    Locked
                } else {
                    Unlocked(locksAt = locksAt)
                }
            }
        }
    }
}
