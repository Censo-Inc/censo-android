package co.censo.censo.presentation.accept_beneficiary

import co.censo.shared.data.Resource
import co.censo.shared.data.model.AcceptBeneficiaryInvitationApiResponse

data class AcceptBeneficiaryInvitationState(
    val invitationId: String = "",
    val userLoggedIn: Boolean = false,
    val acceptBeneficiaryUIState: AcceptBeneficiaryUIState = AcceptBeneficiaryUIState.Welcome,
    val facetecInProgress: Boolean = false,
    val navigateToSignIn: Resource<Unit> = Resource.Uninitialized,
    val badLinkPasted: Resource<Unit> = Resource.Uninitialized,
    val navigateToBeneficiary: Resource<Unit> = Resource.Uninitialized,
    val triggerDeleteUserDialog: Resource<Unit> = Resource.Uninitialized,
    val deleteUserResource: Resource<Unit> = Resource.Uninitialized,
    val linkError: Boolean = false,
)

enum class AcceptBeneficiaryUIState {
    Welcome, FacetecInfo
}