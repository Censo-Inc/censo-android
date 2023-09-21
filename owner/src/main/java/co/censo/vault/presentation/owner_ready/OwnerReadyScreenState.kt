package co.censo.vault.presentation.owner_ready

import Base58EncodedIntermediatePublicKey
import Base58EncodedMasterPublicKey
import Base64EncodedData
import co.censo.shared.data.Resource
import co.censo.shared.data.model.LockApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.Policy
import co.censo.shared.data.model.UnlockApiResponse
import co.censo.shared.data.model.Vault
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class OwnerReadyScreenState(
    val ownerState: OwnerState.Ready = OwnerState.Ready(
        policy = Policy(
            createdAt = Clock.System.now(),
            guardians = emptyList(),
            threshold = 0U,
            encryptedMasterKey = Base64EncodedData(""),
            intermediateKey = Base58EncodedIntermediatePublicKey("")
        ),
        vault = Vault(
            secrets = emptyList(),
            publicMasterEncryptionKey = Base58EncodedMasterPublicKey(""),
        ),
        unlockedForSeconds = null
    ),
    val lockStatus: LockStatus = LockStatus.fromOwnerState(ownerState)
) {
    sealed class LockStatus {
        object Locked: LockStatus()
        data class Unlocked(val locksIn: Duration): LockStatus()
        data class UnlockInProgress(val apiCall: Resource<UnlockApiResponse>): LockStatus()
        data class LockInProgress(val apiCall: Resource<LockApiResponse>): LockStatus()

        companion object {
            fun fromOwnerState(ownerState: OwnerState.Ready): LockStatus =
                when (val unlockedForSeconds = ownerState.unlockedForSeconds) {
                    null -> Locked
                    else -> Unlocked(locksIn = unlockedForSeconds.toInt().seconds)
                }
        }
    }

    fun updateOwnerState(ownerState: OwnerState.Ready): OwnerReadyScreenState =
        copy(
            ownerState = ownerState,
            lockStatus = LockStatus.fromOwnerState(ownerState)
        )
}
