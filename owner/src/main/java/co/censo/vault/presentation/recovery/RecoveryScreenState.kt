package co.censo.vault.presentation.recovery

import VaultSecretId
import co.censo.shared.data.Resource
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.InitiateRecoveryApiResponse
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.Recovery

data class RecoveryScreenState(
    // owner state
    val secrets: List<VaultSecretId> = listOf(),
    val guardians: List<Guardian.TrustedGuardian> = listOf(),
    val recovery: Recovery? = null,

    // recovery control
    val initiateNewRecovery: Boolean = false,

    // api requests
    val ownerStateResource: Resource<OwnerState.Ready> = Resource.Uninitialized,
    val initiateRecoveryResource: Resource<InitiateRecoveryApiResponse> = Resource.Uninitialized,

    // navigation
    val navigationResource: Resource<String> = Resource.Uninitialized,
) {

    val loading = ownerStateResource is Resource.Loading || initiateRecoveryResource is Resource.Loading
    val asyncError = ownerStateResource is Resource.Error || initiateRecoveryResource is Resource.Error

}