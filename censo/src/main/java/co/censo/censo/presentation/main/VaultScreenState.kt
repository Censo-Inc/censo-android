package co.censo.censo.presentation.main

import co.censo.censo.presentation.enter_phrase.EnterPhraseState
import co.censo.shared.data.Resource
import co.censo.shared.data.model.DeleteAccessApiResponse
import co.censo.shared.data.model.DeletePolicySetupApiResponse
import co.censo.shared.data.model.DeleteSeedPhraseApiResponse
import co.censo.shared.data.model.GetOwnerUserApiResponse
import co.censo.shared.data.model.LockApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.SeedPhrase
import co.censo.shared.data.model.TimelockApiResponse
import co.censo.shared.data.model.UpdateSeedPhraseApiResponse

data class VaultScreenState(
    // owner state
    val ownerState: OwnerState.Ready? = null,

    val triggerDeleteUserDialog: Resource<Unit> = Resource.Uninitialized,
    val triggerDeletePhraseDialog: Resource<SeedPhrase> = Resource.Uninitialized,
    val showRenamePhrase: Resource<SeedPhrase> = Resource.Uninitialized,

    // toast
    val syncCloudAccessMessage: Resource<SyncCloudAccessMessage> = Resource.Uninitialized,
    val showDeletePolicySetupConfirmationDialog: Boolean = false,

    //UI
    val showAddApproversUI: Resource<Unit> = Resource.Uninitialized,

    // api requests
    val userResponse: Resource<GetOwnerUserApiResponse> = Resource.Uninitialized,
    val deleteSeedPhraseResource: Resource<DeleteSeedPhraseApiResponse> = Resource.Uninitialized,
    val updateSeedPhraseResource: Resource<UpdateSeedPhraseApiResponse> = Resource.Uninitialized,
    val deleteUserResource: Resource<Unit> = Resource.Uninitialized,
    val lockResponse : Resource<LockApiResponse> = Resource.Uninitialized,
    val resyncCloudAccessRequest : Boolean = false,
    val deletePolicySetup: Resource<DeletePolicySetupApiResponse> = Resource.Uninitialized,
    val enableTimelockResource : Resource<TimelockApiResponse> = Resource.Uninitialized,
    val disableTimelockResource : Resource<TimelockApiResponse> = Resource.Uninitialized,
    val cancelDisableTimelockResource : Resource<Unit> = Resource.Uninitialized,
    val triggerCancelDisableTimelockDialog: Resource<Unit> = Resource.Uninitialized,
    val triggerCancelAccessDialog: Resource<Unit> = Resource.Uninitialized,
    val cancelAccessResource : Resource<DeleteAccessApiResponse> = Resource.Uninitialized,
    val removeApprovers: Resource<Unit> = Resource.Uninitialized,

    // navigation
    val kickUserOut: Resource<Unit> = Resource.Uninitialized,

    //UI
    val showPushNotificationsUI: Resource<Unit> = Resource.Uninitialized,

    // rename seed phrase
    val label: String = "",
    val labelTooLong: String? = null,
) {

    val seedPhrasesSize = ownerState?.vault?.seedPhrases?.size ?: 0

    val loading = userResponse is Resource.Loading ||
            deleteSeedPhraseResource is Resource.Loading ||
            updateSeedPhraseResource is Resource.Loading ||
            deleteUserResource is Resource.Loading ||
            lockResponse is Resource.Loading ||
            deletePolicySetup is Resource.Loading ||
            enableTimelockResource is Resource.Loading ||
            disableTimelockResource is Resource.Loading ||
            cancelDisableTimelockResource is Resource.Loading

    val asyncError = userResponse is Resource.Error ||
                deleteSeedPhraseResource is Resource.Error ||
                updateSeedPhraseResource is Resource.Error ||
                deleteUserResource is Resource.Error ||
                lockResponse is Resource.Error ||
                deletePolicySetup is Resource.Error ||
                enableTimelockResource is Resource.Error ||
                disableTimelockResource is Resource.Error ||
                cancelDisableTimelockResource is Resource.Error ||
                cancelAccessResource is Resource.Error ||
                showAddApproversUI is Resource.Error ||
                removeApprovers is Resource.Error

    val labelIsTooLong = label.length > EnterPhraseState.PHRASE_LABEL_MAX_LENGTH
    val labelValid = label.isNotEmpty() && !labelIsTooLong

    val showClose = showAddApproversUI is Resource.Success || showRenamePhrase is Resource.Success
}

enum class SyncCloudAccessMessage {
    ALREADY_GRANTED, ACCESS_GRANTED, ACCESS_AUTH_FAILED
}