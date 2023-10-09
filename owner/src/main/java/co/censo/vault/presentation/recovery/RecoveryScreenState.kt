package co.censo.vault.presentation.recovery

import VaultSecretId
import co.censo.shared.data.Resource
import co.censo.shared.data.model.DeleteRecoveryApiResponse
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.InitiateRecoveryApiResponse
import co.censo.shared.data.model.Recovery

data class RecoveryScreenState(
    // owner state
    val secrets: List<VaultSecretId> = listOf(),
    val guardians: List<Guardian.TrustedGuardian> = listOf(),
    val recovery: Recovery? = null,
    val approvalsCollected: Int = 0,
    val approvalsRequired: Int = 0,

    // recovery control
    val initiateNewRecovery: Boolean = false,

    // api requests
    val userResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val initiateRecoveryResource: Resource<InitiateRecoveryApiResponse> = Resource.Uninitialized,
    val cancelRecoveryResource: Resource<DeleteRecoveryApiResponse> = Resource.Uninitialized,

    // navigation
    val navigationResource: Resource<String> = Resource.Uninitialized,
) {

    val loading = userResponse is Resource.Loading || initiateRecoveryResource is Resource.Loading || cancelRecoveryResource is Resource.Loading
    val asyncError = userResponse is Resource.Error || initiateRecoveryResource is Resource.Error || cancelRecoveryResource is Resource.Error

}