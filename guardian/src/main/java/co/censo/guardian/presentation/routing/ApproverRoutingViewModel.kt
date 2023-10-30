package co.censo.guardian.presentation.routing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.guardian.presentation.home.GuardianUIState
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GuardianPhase
import co.censo.shared.data.model.GuardianState
import co.censo.shared.data.model.forParticipant
import co.censo.shared.data.repository.GuardianRepository
import co.censo.shared.data.repository.OwnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 *
 * 1. Retrieve user data from API
 * 2. Determine approver: determineApprover()
 *
 *      Backend will know who we are based on our Google ID token
 *
 *      EXCEPT: For initial onboarding when we need to create the approver --> Invitation Id
 *
 *      Access Edge Case: Cannot be null for access b/c we should have participant ID
 *                        and the backend will know our account by Google Id token
 *
 *
 * 3. Determine to send user to Onboarding or Access Flows
 *      Null: Initial Onboarding User
 *      Complete: Home Screen (Unknown right now with designs)
 *      Onboarding Sub Type: Onboarding
 *      Access Sub Type: Access
 *
 */

@HiltViewModel
class ApproverRoutingViewModel @Inject constructor(
    private val guardianRepository: GuardianRepository,
    private val ownerRepository: OwnerRepository,
) : ViewModel() {
    var state by mutableStateOf(ApproverRoutingState())
        private set

    fun onStart() {
        retrieveApproverState(false)
    }

    fun onStop() {
        state = ApproverRoutingState()
    }

    fun retrieveApproverState(silently: Boolean) {
        if (!silently) {
            state = state.copy(userResponse = Resource.Loading())
        }

        viewModelScope.launch {
            val userResponse = ownerRepository.retrieveUser()

            state = state.copy(userResponse = userResponse)

            if (userResponse is Resource.Success) {
                determineApprover(userResponse.data!!.guardianStates)
            }
        }
    }

    private fun determineApprover(guardianStates: List<GuardianState>) {
        val participantId = guardianRepository.retrieveParticipantId()

        if (participantId.isEmpty()) {
            determineApproverUIState(guardianStates.firstOrNull())
            return
        }

        val guardianState = guardianStates.forParticipant(participantId)
        if (guardianState == null) {
            handleMissingApprover()
            return
        }

        determineApproverUIState(guardianState)
    }

    private fun handleMissingApprover() {
        guardianRepository.clearParticipantId()
        state = state.copy(guardianUIState = GuardianUIState.INVALID_PARTICIPANT_ID)
        triggerNavigation(RoutingDestination.ACCESS)
    }


    private fun determineApproverUIState(guardianState: GuardianState?) {
        if (guardianState == null) {
            triggerNavigation(RoutingDestination.ONBOARDING)
            return
        }

        val approverDestination = when (guardianState.phase) {
            //No Action needed
            GuardianPhase.Complete -> RoutingDestination.ACCESS

            //Approver Access Requested
            is GuardianPhase.RecoveryRequested,
            is GuardianPhase.RecoveryVerification,
            is GuardianPhase.RecoveryConfirmation -> RoutingDestination.ACCESS

            //Approver Onboarding
            is GuardianPhase.WaitingForCode,
            is GuardianPhase.WaitingForVerification,
            is GuardianPhase.VerificationRejected -> RoutingDestination.ONBOARDING
        }

        //Trigger navigation to move the approver forward
        triggerNavigation(routingDestination = approverDestination)
    }

    private fun triggerNavigation(routingDestination: RoutingDestination) {
        state = when (routingDestination) {
            RoutingDestination.ACCESS ->
                state.copy(navToGuardianHome = Resource.Success(Unit))

            RoutingDestination.ONBOARDING ->
                state.copy(navToApproverOnboarding = Resource.Success(Unit))
        }
    }

    fun resetGuardianHomeNavigationTrigger() {
        state = state.copy(navToGuardianHome = Resource.Uninitialized)
    }

    fun resetApproverOnboardingNavigationTrigger() {
        state = state.copy(navToApproverOnboarding = Resource.Uninitialized)
    }
}