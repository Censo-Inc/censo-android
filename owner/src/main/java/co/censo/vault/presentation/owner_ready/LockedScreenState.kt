package co.censo.vault.presentation.owner_ready

import co.censo.shared.data.Resource
import co.censo.shared.data.model.LockApiResponse
import co.censo.shared.data.model.UnlockApiResponse
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class LockedScreenState(
    var lockStatus: LockStatus = LockStatus.Locked
) {

    sealed class LockStatus {
        object Locked : LockStatus()
        data class Unlocked(val locksAt: Instant) : LockStatus()
        data class UnlockInProgress(val apiCall: Resource<UnlockApiResponse>) : LockStatus()
        data class LockInProgress(val apiCall: Resource<LockApiResponse>) : LockStatus()

        companion object {
            fun fromInstant(locksAt: Instant?): LockStatus =
                if (locksAt == null || Clock.System.now() > locksAt) {
                    Locked
                } else {
                    Unlocked(locksAt = locksAt)
                }
        }
    }
}
