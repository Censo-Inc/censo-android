package co.censo.vault.presentation.recovery

import ParticipantId
import VaultSecretId
import co.censo.shared.data.Resource
import co.censo.shared.data.model.DeleteRecoveryApiResponse
import co.censo.shared.data.model.GetUserApiResponse
import co.censo.shared.data.model.Guardian
import co.censo.shared.data.model.InitiateRecoveryApiResponse
import co.censo.shared.data.model.Recovery
import co.censo.shared.data.model.SubmitRecoveryTotpVerificationApiResponse

data class RecoveryScreenState(
    // owner state
    val secrets: List<VaultSecretId> = listOf(),
    val guardians: List<Guardian.TrustedGuardian> = listOf(),
    val recovery: Recovery? = null,
    val approvalsCollected: Int = 0,
    val approvalsRequired: Int = 0,

    // recovery control
    val initiateNewRecovery: Boolean = false,

    // totp code verification
    val totpVerificationCode: String = "",
    val totpVerificationWaitingForApproval: Boolean = false,
    val totpVerificationRejected: Boolean = false,

    // api requests
    val userResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val approvalsResponse: Resource<GetUserApiResponse> = Resource.Uninitialized,
    val initiateRecoveryResource: Resource<InitiateRecoveryApiResponse> = Resource.Uninitialized,
    val cancelRecoveryResource: Resource<DeleteRecoveryApiResponse> = Resource.Uninitialized,
    val submitTotpVerificationResource: Resource<SubmitRecoveryTotpVerificationApiResponse> = Resource.Uninitialized,

    // UI state
    val recoveryUIState: RecoveryUIState = RecoveryUIState.Main,

    // navigation
    val navigationResource: Resource<String> = Resource.Uninitialized,
) {

    val loading = userResponse is Resource.Loading
            || initiateRecoveryResource is Resource.Loading
            || cancelRecoveryResource is Resource.Loading
            || submitTotpVerificationResource is Resource.Loading

    val asyncError = userResponse is Resource.Error
            || initiateRecoveryResource is Resource.Error
            || cancelRecoveryResource is Resource.Error
            || submitTotpVerificationResource is Resource.Loading
}

sealed class RecoveryUIState {
    object Main : RecoveryUIState()
    data class EnterVerificationCodeState(
        val approverLabel: String,
        val participantId: ParticipantId
    ) : RecoveryUIState()
}