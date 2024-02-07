package co.censo.approver.presentation.entrance

import android.net.Uri
import co.censo.shared.data.Resource
import co.censo.shared.util.NavigationData

data class ApproverEntranceState(
    val uiState: ApproverEntranceUIState = ApproverEntranceUIState.Initial,
    val linkError: Boolean = false,
    val loggedIn: Boolean = false,

    val triggerGoogleSignIn: Resource<Unit> = Resource.Uninitialized,
    val signInUserResource: Resource<Unit> = Resource.Uninitialized,

    val deleteUserResource: Resource<Unit> = Resource.Uninitialized,
    val navigationResource: Resource<NavigationData> = Resource.Uninitialized,
    val appLinkUri: Uri? = null
) {
    val isLoading = signInUserResource is Resource.Loading

    val apiCallErrorOccurred = signInUserResource is Resource.Error
            || triggerGoogleSignIn is Resource.Error
}

enum class RoutingDestination {
    ONBOARDING, ACCESS, AUTH_RESET
}

sealed class ApproverEntranceUIState {

    object Initial : ApproverEntranceUIState()

    class Landing(val isActiveApprover: Boolean) : ApproverEntranceUIState()

    object LoggedOutPasteLink : ApproverEntranceUIState()

    object SignIn : ApproverEntranceUIState()

    class LoggedInPasteLink(val isActiveApprover: Boolean) : ApproverEntranceUIState()
}