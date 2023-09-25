package co.censo.vault.presentation.owner_ready

import co.censo.shared.data.model.LockStatus
import co.censo.shared.data.model.OwnerState

data class OwnerReadyScreenState(
    val ownerState: OwnerState.Ready? = null,
    val lockStatus: LockStatus = ownerState?.let { LockStatus.fromOwnerState(it) } ?: LockStatus.Locked
) {

    fun updateOwnerState(ownerState: OwnerState.Ready): OwnerReadyScreenState =
        copy(
            ownerState = ownerState,
            lockStatus = LockStatus.fromOwnerState(ownerState)
        )
}
