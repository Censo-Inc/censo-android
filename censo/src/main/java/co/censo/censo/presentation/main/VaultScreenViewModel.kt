package co.censo.censo.presentation.main

import Base64EncodedData
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.censo.presentation.Screen
import co.censo.shared.data.Resource
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.ApproverStatus
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.SubscriptionStatus
import co.censo.shared.data.model.SeedPhrase
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.data.repository.PushRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultScreenViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val ownerStateFlow: MutableStateFlow<Resource<OwnerState>>,
    private val pushRepository: PushRepository
) : ViewModel() {

    var state by mutableStateOf(VaultScreenState())
        private set

    fun onStart() {
        retrieveOwnerState()
    }

    fun userHasSeenPushDialog() = pushRepository.userHasSeenPushDialog()

    fun retrieveOwnerState() {
        state = state.copy(userResponse = Resource.Loading())

        viewModelScope.launch {
            val response = ownerRepository.retrieveUser()

            state = state.copy(
                userResponse = response
            )

            if (response is Resource.Success) {
                onOwnerState(response.data!!.ownerState)
            }
        }
    }

    fun deleteSeedPhrase() {
        if (state.triggerEditPhraseDialog !is Resource.Success && state.triggerEditPhraseDialog.data == null) {
            state = state.copy(
                deleteSeedPhraseResource = Resource.Error()
            )
            return
        }

        val seedPhrase = state.triggerEditPhraseDialog.data

        state = state.copy(
            triggerEditPhraseDialog = Resource.Uninitialized,
            deleteSeedPhraseResource = Resource.Loading()
        )

        viewModelScope.launch {
            val response = ownerRepository.deleteSeedPhrase(seedPhrase!!.guid)

            state = state.copy(
                deleteSeedPhraseResource = response
            )

            if (response is Resource.Success) {
                onOwnerState(response.data!!.ownerState)
            }
        }
    }

    fun determinePolicyModificationRoute(): String {
        val ownerState = state.ownerState
        return if (ownerState is OwnerState.Ready && ownerState.policySetup?.approvers?.all
            { it.status is ApproverStatus.Confirmed } == true
        ) {
            //If all approvers are confirmed then move directly to replacing the policy
            Screen.ReplacePolicyRoute.addApproversRoute()
        } else {
            Screen.PolicySetupRoute.addApproversRoute()
        }
    }

    fun deleteUser() {
        state = state.copy(
            deleteUserResource = Resource.Loading()
        )

        val participantId =
            state.ownerState?.policy?.approvers?.first { it.isOwner }?.participantId

        viewModelScope.launch(Dispatchers.IO) {
            val response = ownerRepository.deleteUser(participantId)

            state = state.copy(
                deleteUserResource = response
            )

            if (response is Resource.Success) {
                onOwnerState(OwnerState.Initial(Base64EncodedData(""), subscriptionStatus = SubscriptionStatus.Active))
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

    private fun onOwnerState(ownerState: OwnerState) {
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

        ownerStateFlow.tryEmit(Resource.Success(ownerState))
    }

    fun showAddApproverUI() {
        state = state.copy(showAddApproversUI = Resource.Success(Unit))
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

    fun showEditPhraseDialog(seedPhrase: SeedPhrase) {
        state = state.copy(triggerEditPhraseDialog = Resource.Success(seedPhrase))
    }

    fun onCancelDeletePhrase() {
        state = state.copy(triggerEditPhraseDialog = Resource.Uninitialized)
    }

    fun onCancelResetUser() {
        state = state.copy(triggerDeleteUserDialog = Resource.Uninitialized)
    }

    fun reset() {
        state = VaultScreenState()
    }

    fun lock() {
        state = state.copy(lockResponse = Resource.Loading())
        viewModelScope.launch {
            val lockResponse = ownerRepository.lock().map { it.ownerState }

            if (lockResponse is Resource.Success) {
                lockResponse.data?.let { onOwnerState(it) }
                resetLockResource()
            }

            if (lockResponse is Resource.Error) {
                state =
                    state.copy(
                        lockResponse = Resource.Error(
                            exception = lockResponse.exception,
                            errorCode = lockResponse.errorCode
                        )
                    )
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
        state = state.copy(deletePolicySetup = Resource.Loading())
        hideDeletePolicySetupConfirmationDialog()

        viewModelScope.launch {
            val response = ownerRepository.deletePolicySetup()

            if (response is Resource.Success) {
                response.data?.let { onOwnerState(it.ownerState) }
            }

            state = state.copy(deletePolicySetup = response)
        }
    }

    fun resetDeletePolicySetupResource() {
        state = state.copy(deletePolicySetup = Resource.Uninitialized)
    }

}
