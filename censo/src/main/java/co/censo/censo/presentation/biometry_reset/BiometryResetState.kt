package co.censo.censo.presentation.biometry_reset

import ParticipantId
import co.censo.shared.data.Resource
import co.censo.shared.data.model.Approver
import co.censo.shared.data.model.OwnerState
import co.censo.shared.data.model.AuthenticationResetApproval
import co.censo.shared.data.model.CancelAuthenticationResetApiResponse
import co.censo.shared.data.model.InitiateAuthenticationResetApiResponse
import co.censo.shared.data.model.ReplaceAuthenticationApiResponse
import co.censo.shared.util.NavigationData

data class BiometryResetState(
    // UI state
    val uiState: UIState = UIState.Initial,
    val selectedApprover: Approver.TrustedApprover? = null,
    val showCancelConfirmationDialog: Boolean = false,

    // data
    val authResetInitiated: Boolean = false,
    val ownerState: OwnerState.Ready? = null,
    val approvers: List<Approver.TrustedApprover> = listOf(),
    val approvals: List<AuthenticationResetApproval> = listOf(),

    // totp
    val secondsLeft: Int = 0,
    val counter: Long = 0,
    val approverCodes: Map<ParticipantId, String> = emptyMap(),

    // api requests
    val userResponse: Resource<OwnerState> = Resource.Uninitialized,
    val initiateBiometryResetResource: Resource<InitiateAuthenticationResetApiResponse> = Resource.Uninitialized,
    val cancelBiometryResetResource: Resource<CancelAuthenticationResetApiResponse> = Resource.Uninitialized,
    val replaceBiometryResource: Resource<ReplaceAuthenticationApiResponse> = Resource.Uninitialized,

    // navigation
    val navigationResource: Resource<NavigationData> = Resource.Uninitialized,
) {

    val loading = userResponse is Resource.Loading
            || initiateBiometryResetResource is Resource.Loading
            || cancelBiometryResetResource is Resource.Loading
            || replaceBiometryResource is Resource.Loading

    val asyncError = userResponse is Resource.Error
            || initiateBiometryResetResource is Resource.Error
            || cancelBiometryResetResource is Resource.Error
            || replaceBiometryResource is Resource.Error
}


enum class UIState {
    Initial, AnotherDevice, SelectApprover, ApproveBiometryReset, EnrollNewBiometry, Facetec, Completed;

    fun notInList(vararg states: UIState): Boolean {
        return !states.contains(this)
    }
}


sealed interface BiometryResetAction {
    data object InitiateBiometryReset : BiometryResetAction
    data object CancelBiometryReset : BiometryResetAction
    data object CancelBiometryResetCancelled : BiometryResetAction

    data class ApproverSelectionChanged(val approver: Approver.TrustedApprover) : BiometryResetAction
    data object ContinueWithSelectedApprover : BiometryResetAction
    data object AuthResetApproved : BiometryResetAction
    data object EnrollNewBiometry : BiometryResetAction
    data object Completed : BiometryResetAction

    data object Retry : BiometryResetAction
    data object BackClicked : BiometryResetAction
    data object FacetecCancelled : BiometryResetAction
}