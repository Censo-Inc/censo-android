package co.censo.censo.presentation.main

import co.censo.shared.data.Resource
import co.censo.shared.data.model.DeletePolicySetupApiResponse
import co.censo.shared.data.model.DeleteSeedPhraseApiResponse
import co.censo.shared.data.model.GetOwnerUserApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.SeedPhrase

data class VaultScreenState(
    // owner state
    val ownerState: OwnerState.Ready? = null,

    val triggerDeleteUserDialog: Resource<Unit> = Resource.Uninitialized,
    val triggerEditPhraseDialog: Resource<SeedPhrase> = Resource.Uninitialized,

    // toast
    val syncCloudAccessMessage: Resource<SyncCloudAccessMessage> = Resource.Uninitialized,
    val showDeletePolicySetupConfirmationDialog: Boolean = false,

    //UI
    val showAddApproversUI: Resource<Unit> = Resource.Uninitialized,

    // api requests
    val userResponse: Resource<GetOwnerUserApiResponse> = Resource.Uninitialized,
    val deleteSeedPhraseResource: Resource<DeleteSeedPhraseApiResponse> = Resource.Uninitialized,
    val deleteUserResource: Resource<Unit> = Resource.Uninitialized,
    val lockResponse : Resource<Unit> = Resource.Uninitialized,
    val resyncCloudAccessRequest : Boolean = false,
    val deletePolicySetup: Resource<DeletePolicySetupApiResponse> = Resource.Uninitialized,

    // navigation
    val kickUserOut: Resource<Unit> = Resource.Uninitialized,
) {

    val externalApprovers = (ownerState?.policy?.approvers?.size?.minus(1)) ?: 0
    val seedPhrasesSize = ownerState?.vault?.seedPhrases?.size ?: 0

    val loading = userResponse is Resource.Loading ||
            deleteSeedPhraseResource is Resource.Loading ||
            deleteUserResource is Resource.Loading ||
            lockResponse is Resource.Loading ||
            deletePolicySetup is Resource.Loading

    val asyncError = userResponse is Resource.Error ||
                deleteSeedPhraseResource is Resource.Error ||
                deleteUserResource is Resource.Error ||
                lockResponse is Resource.Error ||
                deletePolicySetup is Resource.Error

}

enum class SyncCloudAccessMessage {
    ALREADY_GRANTED, ACCESS_GRANTED, ACCESS_AUTH_FAILED
}