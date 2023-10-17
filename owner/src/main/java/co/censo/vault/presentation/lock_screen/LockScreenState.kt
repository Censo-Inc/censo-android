package co.censo.vault.presentation.lock_screen

import co.censo.shared.data.Resource
import co.censo.shared.data.model.LockApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.UnlockApiResponse
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class LockScreenState(
    val ownerStateResource: Resource<OwnerState> = Resource.Uninitialized,
    val lockStatus: LockStatus = LockStatus.None
) {
    val unlocked = lockStatus is LockStatus.Unlocked

    sealed class LockStatus {
        object None : LockStatus()
        object Locked : LockStatus()
        data class Unlocked(val locksAt: Instant) : LockStatus()
        data class UnlockInProgress(val apiCall: Resource<UnlockApiResponse>) : LockStatus()

        companion object {
            fun fromOwnerState(ownerState: OwnerState): LockStatus =
                when (ownerState) {
                    is OwnerState.GuardianSetup -> fromLocksAt(ownerState.locksAt)
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
