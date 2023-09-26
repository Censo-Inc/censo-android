package co.censo.vault.presentation.owner_ready

import co.censo.shared.data.Resource
import co.censo.shared.data.model.LockApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.UnlockApiResponse
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime

data class OwnerReadyScreenState(
    val ownerState: OwnerState.Ready? = null,
    val lockStatus: LockStatus = ownerState?.let { LockStatus.fromOwnerState(it) }
        ?: LockStatus.Locked,
    val locksAt: LocalDateTime? = null
) {

    sealed class LockStatus {
        object Locked : LockStatus()
        data class Unlocked(val locksAt: Instant) : LockStatus()
        data class UnlockInProgress(val apiCall: Resource<UnlockApiResponse>) : LockStatus()
        data class LockInProgress(val apiCall: Resource<LockApiResponse>) : LockStatus()

        companion object {
            fun fromOwnerState(ownerState: OwnerState.Ready): LockStatus =
                when (val locksAt = ownerState.locksAt) {
                    null -> Locked
                    else -> Unlocked(locksAt = locksAt)
                }
        }
    }

    fun updateOwnerState(ownerState: OwnerState.Ready): OwnerReadyScreenState =
        copy(
            ownerState = ownerState,
            lockStatus = LockStatus.fromOwnerState(ownerState)
        )
}
