package co.censo.censo.presentation.main

import Base64EncodedData
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.censo.presentation.Screen
import co.censo.shared.data.Resource
import co.censo.shared.data.model.Access
import co.censo.shared.data.model.AccessStatus
import co.censo.shared.data.model.ApproverStatus
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.SubscriptionStatus
import co.censo.shared.data.model.SeedPhrase
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.data.repository.PushRepository
import co.censo.shared.util.VaultCountDownTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds


enum class AccessButtonLabelEnum {
    BeginAccess,
    RequestAccess,
    CancelAccess,
    ShowSeedPhrases
}

@HiltViewModel
class VaultScreenViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val pushRepository: PushRepository,
    private val timer: VaultCountDownTimer
) : ViewModel() {

    var state by mutableStateOf(VaultScreenState())
        private set

    fun onStart() {
        viewModelScope.launch {
            onOwnerState(ownerRepository.getOwnerStateValue(), updateGlobalState = false)
        }
    }

    fun onResume() {
        timer.start(interval = 5.seconds.inWholeMilliseconds) {
            (accessTimelockExpiration() ?: state.ownerState?.timelockSetting?.disabledAt)?.let { expiresAt ->
                if (expiresAt < Clock.System.now()) {
                    retrieveOwnerState(silently = true)
                }
            }
        }
    }

    fun onPause() {
        timer.stop()
    }

    fun userHasSeenPushDialog() = pushRepository.userHasSeenPushDialog()

    fun retrieveOwnerState(silently: Boolean = false) {
        if (!silently) {
            state = state.copy(userResponse = Resource.Loading)
        }

        viewModelScope.launch {
            val response = ownerRepository.retrieveUser()
            if (!silently) {
                state = state.copy(
                    userResponse = response
                )
            }

            if (response is Resource.Success) {
                onOwnerState(response.data.ownerState)
            }
        }
    }

    fun deleteSeedPhrase() {
        if (state.triggerDeletePhraseDialog !is Resource.Success) {
            state = state.copy(
                deleteSeedPhraseResource = Resource.Error()
            )
            return
        }

        val seedPhrase = state.triggerDeletePhraseDialog.asSuccess().data

        state = state.copy(
            triggerDeletePhraseDialog = Resource.Uninitialized,
            deleteSeedPhraseResource = Resource.Loading
        )

        viewModelScope.launch {
            val response = ownerRepository.deleteSeedPhrase(seedPhrase.guid)

            state = state.copy(
                deleteSeedPhraseResource = response
            )

            if (response is Resource.Success) {
                onOwnerState(response.data.ownerState)
            }
        }
    }

    fun renameSeedPhrase() {
        if (state.showRenamePhrase !is Resource.Success) {
            state = state.copy(
                updateSeedPhraseResource = Resource.Error()
            )
            return
        }

        val seedPhrase = state.showRenamePhrase.asSuccess().data

        state = state.copy(
            showRenamePhrase = Resource.Uninitialized,
            updateSeedPhraseResource = Resource.Loading
        )

        viewModelScope.launch {
            val response = ownerRepository.updateSeedPhrase(seedPhrase.guid, state.label)

            state = state.copy(
                updateSeedPhraseResource = response
            )

            if (response is Resource.Success) {
                onOwnerState(response.data.ownerState)
                state = state.copy(label = "")
            }
        }
    }

    fun resetUpdateSeedPhraseResponse() {
        state = state.copy(updateSeedPhraseResource = Resource.Uninitialized)
    }

    fun resetShowRenamePhase() {
        state = state.copy(showRenamePhrase = Resource.Uninitialized)
    }

    fun determinePolicyModificationRoute(): String {
        val ownerState = state.ownerState
        return if (ownerState is OwnerState.Ready && ownerState.policySetup?.approvers?.all
            { it.status is ApproverStatus.Confirmed } == true
        ) {
            //If all approvers are confirmed then move directly to replacing the policy
            Screen.ReplacePolicyRoute.buildNavRoute(addApprovers = true)
        } else {
            Screen.PolicySetupRoute.addApproversRoute()
        }
    }

    fun deleteUser() {
        state = state.copy(
            deleteUserResource = Resource.Loading
        )

        val participantId =
            state.ownerState?.policy?.approvers?.first { it.isOwner }?.participantId

        viewModelScope.launch(Dispatchers.IO) {
            val response = ownerRepository.deleteUser(participantId)

            state = state.copy(
                deleteUserResource = response
            )

            if (response is Resource.Success) {
                onOwnerState(
                    OwnerState.Empty
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            ownerRepository.signUserOut()
            state = state.copy(kickUserOut = Resource.Success(Unit))
        }
    }

    fun showPushNotificationsUI() {
        state = state.copy(showPushNotificationsUI = Resource.Success(Unit))
    }

    fun resetShowPushNotificationsUI() {
        state = state.copy(showPushNotificationsUI = Resource.Uninitialized)
    }

    fun determineAccessButtonLabel(): AccessButtonLabelEnum {
        return state.ownerState?.let {
            val beginOrRequest = if (it.policy.approvers.count() > 1) {
                AccessButtonLabelEnum.RequestAccess
            } else {
                AccessButtonLabelEnum.BeginAccess
            }
            if (it.access != null && it.access is Access.ThisDevice) {
                when ((it.access as Access.ThisDevice).status) {
                    AccessStatus.Available -> AccessButtonLabelEnum.ShowSeedPhrases
                    AccessStatus.Timelocked -> AccessButtonLabelEnum.CancelAccess
                    else -> beginOrRequest
                }
            } else {
                beginOrRequest
            }
        } ?: AccessButtonLabelEnum.BeginAccess
    }

    fun accessButtonEnabled(): Boolean {
        return state.ownerState?.vault?.seedPhrases?.isNotEmpty() ?: false
    }

    fun enableTimelock() {
        state = state.copy(
            enableTimelockResource = Resource.Loading
        )

        viewModelScope.launch(Dispatchers.IO) {
            val response = ownerRepository.enableTimelock()

            state = state.copy(
                enableTimelockResource = response
            )

            if (response is Resource.Success) {
                onOwnerState(response.data.ownerState)
            }
        }
    }

    fun disableTimelock() {
        state = state.copy(
            disableTimelockResource = Resource.Loading
        )

        viewModelScope.launch(Dispatchers.IO) {
            val response = ownerRepository.disableTimelock()

            state = state.copy(
                disableTimelockResource = response
            )

            if (response is Resource.Success) {
                onOwnerState(response.data.ownerState)
            }
        }
    }

    fun cancelDisableTimelock() {
        state = state.copy(
            cancelDisableTimelockResource = Resource.Loading,
            triggerCancelDisableTimelockDialog = Resource.Uninitialized
        )

        viewModelScope.launch(Dispatchers.IO) {
            val response = ownerRepository.cancelDisableTimelock()

            state = state.copy(
                cancelDisableTimelockResource = response
            )

            if (response is Resource.Success) {
                retrieveOwnerState()
            }
        }
    }

    fun cancelAccess() {
        state = state.copy(
            cancelAccessResource = Resource.Loading,
            triggerCancelAccessDialog = Resource.Uninitialized
        )

        viewModelScope.launch {
            val response = ownerRepository.cancelAccess()

            if (response is Resource.Success) {
                onOwnerState(response.data.ownerState)
            }

            state = state.copy(cancelAccessResource = response)
        }
    }

    private fun onOwnerState(ownerState: OwnerState, updateGlobalState: Boolean = true) {
        state = when (ownerState) {
            is OwnerState.Ready -> {
                state.copy(ownerState = ownerState)
            }

            else -> {
                // other owner states are not supported on this view
                // navigate back to start of the app so it can fix itself
                state.copy(kickUserOut = Resource.Success(Unit))
            }
        }

        if (updateGlobalState) {
            ownerRepository.updateOwnerState(ownerState)
        }
    }

    fun accessTimelockExpiration(): Instant? {
        return when (val access = state.ownerState?.access) {
            is Access.ThisDevice -> if (access.status == AccessStatus.Timelocked) {
                access.unlocksAt
            } else null

            else -> null
        }
    }

    fun showAddApproverUI() {
        state = if (state.ownerState?.hasBlockingPhraseAccessRequest() == true) {
            state.copy(showAddApproversUI = Resource.Error(exception = Exception("Cannot setup approvers while an access is in progress")))
        } else {
            state.copy(showAddApproversUI = Resource.Success(Unit))
        }
    }

    fun resetDeleteUserResource() {
        state = state.copy(deleteUserResource = Resource.Uninitialized)
    }

    fun resetShowApproversUI() {
        state = state.copy(showAddApproversUI = Resource.Uninitialized)
    }

    fun resetDeleteSeedPhraseResponse() {
        state = state.copy(deleteSeedPhraseResource = Resource.Uninitialized)
    }

    fun showDeleteUserDialog() {
        state = state.copy(triggerDeleteUserDialog = Resource.Success(Unit))
    }

    fun showDeletePhraseDialog(seedPhrase: SeedPhrase) {
        state = state.copy(
            triggerDeletePhraseDialog = Resource.Success(seedPhrase)
        )
    }

    fun showRenamePhrase(seedPhrase: SeedPhrase) {
        state = state.copy(
            label = seedPhrase.label,
            showRenamePhrase = Resource.Success(seedPhrase),
        )
    }

    fun updateLabel(updatedLabel: String) {
        state = state.copy(
            label = updatedLabel
        )
    }

    fun onCancelDeletePhrase() {
        state = state.copy(triggerDeletePhraseDialog = Resource.Uninitialized)
    }

    fun onCancelResetUser() {
        state = state.copy(triggerDeleteUserDialog = Resource.Uninitialized)
    }

    fun onCancelDisableTimelock() {
        state = state.copy(triggerCancelDisableTimelockDialog = Resource.Success(Unit))
    }

    fun resetCancelDisableTimelockDialog() {
        state = state.copy(triggerCancelDisableTimelockDialog = Resource.Uninitialized)
    }

    fun onCancelAccess() {
        state = state.copy(triggerCancelAccessDialog = Resource.Success(Unit))
    }

    fun resetCancelAccess() {
        state = state.copy(triggerCancelAccessDialog = Resource.Uninitialized)
    }

    fun resetEnableTimelockResource() {
        state = state.copy(enableTimelockResource = Resource.Uninitialized)
    }

    fun resetDisableTimelockResource() {
        state = state.copy(disableTimelockResource = Resource.Uninitialized)
    }

    fun resetCancelDisableTimelockResource() {
        state = state.copy(cancelDisableTimelockResource = Resource.Uninitialized)
    }

    fun setRemoveApproversError() {
        state =
            state.copy(removeApprovers = Resource.Error(exception = Exception("Cannot remove approvers")))
    }

    fun resetRemoveApprovers() {
        state = state.copy(removeApprovers = Resource.Uninitialized)
    }

    fun delayedReset() {
        viewModelScope.launch {
            delay(1000)
            state = VaultScreenState()
        }
    }

    fun lock() {
        state = state.copy(lockResponse = Resource.Loading)
        viewModelScope.launch {
            val lockResponse = ownerRepository.lock()

            if (lockResponse is Resource.Success) {
                onOwnerState(lockResponse.data.ownerState)
                resetLockResource()
            }

            if (lockResponse is Resource.Error) {
                state = state.copy(lockResponse = lockResponse)
            }
        }
    }

    fun resetLockResource() {
        state = state.copy(lockResponse = Resource.Uninitialized)
    }

    fun resyncCloudAccess() {
        state = state.copy(resyncCloudAccessRequest = true)
    }

    fun resetResyncCloudAccessRequest() {
        state = state.copy(resyncCloudAccessRequest = false)
    }

    fun setSyncCloudAccessMessage(syncCloudAccessMessage: SyncCloudAccessMessage) {
        state = state.copy(
            syncCloudAccessMessage = Resource.Success(syncCloudAccessMessage)
        )
    }

    fun resetSyncCloudAccessMessage() {
        state = state.copy(
            syncCloudAccessMessage = Resource.Uninitialized
        )
    }

    fun showDeletePolicySetupConfirmationDialog() {
        state = state.copy(showDeletePolicySetupConfirmationDialog = true)
    }

    fun hideDeletePolicySetupConfirmationDialog() {
        state = state.copy(showDeletePolicySetupConfirmationDialog = false)
    }

    fun deletePolicySetupConfirmed() {
        state = state.copy(deletePolicySetup = Resource.Loading)
        hideDeletePolicySetupConfirmationDialog()

        viewModelScope.launch {
            val response = ownerRepository.deletePolicySetup()

            if (response is Resource.Success) {
                response.data.let { onOwnerState(it.ownerState) }
            }

            state = state.copy(deletePolicySetup = response)
        }
    }

    fun resetDeletePolicySetupResource() {
        state = state.copy(deletePolicySetup = Resource.Uninitialized)
    }

}
