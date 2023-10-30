package co.censo.guardian.presentation.routing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.guardian.presentation.home.GuardianUIState
import co.censo.guardian.routingLogTag
import co.censo.shared.data.Resource
import co.censo.shared.data.model.GuardianPhase
import co.censo.shared.data.model.GuardianState
import co.censo.shared.data.model.forParticipant
import co.censo.shared.data.repository.GuardianRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.projectLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

//Overall flow
//1. get user data
//2. determine guardian state
//3. action step / UI state

@HiltViewModel
class ApproverRoutingViewModel @Inject constructor(
    private val guardianRepository: GuardianRepository,
    private val ownerRepository: OwnerRepository,
) : ViewModel() {
    var state by mutableStateOf(ApproverRoutingState())
        private set

    fun onStart() {
        retrieveApproverState()
    }

    fun onStop() {
        state = ApproverRoutingState()
    }

    fun retrieveApproverState() {
        state = state.copy(userResponse = Resource.Loading())

        silentRetrieveApproverState()
    }

    private fun silentRetrieveApproverState() {
        viewModelScope.launch {
            val userResponse = ownerRepository.retrieveUser()
            projectLog(tag = routingLogTag, message = "User Response: ${userResponse.data}")

            state = state.copy(userResponse = userResponse)

            if (userResponse is Resource.Success) {
                determineApprover(userResponse.data!!.guardianStates)
            }
        }
    }

    private fun determineApprover(guardianStates: List<GuardianState>) {
        val participantId = guardianRepository.retrieveParticipantId()

        if (participantId.isEmpty()) {
            projectLog(tag = routingLogTag, message = "determining guardian UI state with firstOrNull guardianState")
            determineApproverUIState(guardianStates.firstOrNull())
            return
        }

        val guardianState = guardianStates.forParticipant(participantId)
        if (guardianState == null) {
            projectLog(tag = routingLogTag, message = "handling missing guardian")
            handleMissingApprover()
            return
        }

        projectLog(tag = routingLogTag, message = "determining guardian UI state with existing approver guardianState")
        determineApproverUIState(guardianState)
    }

    private fun handleMissingApprover() {
        guardianRepository.clearParticipantId()
        state = state.copy(guardianUIState = GuardianUIState.INVALID_PARTICIPANT_ID)
        triggerNavigation(GuardianUIState.INVALID_PARTICIPANT_ID)
    }


    private fun determineApproverUIState(guardianState: GuardianState?) {
        val inviteCode = guardianRepository.retrieveInvitationId()
        val participantId = guardianRepository.retrieveParticipantId()

        if (guardianState == null) {
            projectLog(
                tag = routingLogTag,
                message = "Determining UI state for new approver onboarding"
            )
            val approverOnboardingUIState = if (inviteCode.isEmpty()) {
                GuardianUIState.MISSING_INVITE_CODE
            } else {
                GuardianUIState.INVITE_READY
            }
            triggerNavigation(approverOnboardingUIState)
            return
        }

        projectLog(
            tag = routingLogTag,
            message = "Determining UI state with following guardianState: $guardianState"
        )

        val existingApproverUIState = when (guardianState.phase) {// existing approver
            // Onboarding, invitationId is mandatory
            is GuardianPhase.WaitingForCode -> if (inviteCode.isEmpty()) GuardianUIState.MISSING_INVITE_CODE else GuardianUIState.WAITING_FOR_CODE
            is GuardianPhase.WaitingForVerification -> if (inviteCode.isEmpty()) GuardianUIState.MISSING_INVITE_CODE else GuardianUIState.WAITING_FOR_CONFIRMATION
            is GuardianPhase.VerificationRejected -> if (inviteCode.isEmpty()) GuardianUIState.MISSING_INVITE_CODE else GuardianUIState.CODE_REJECTED

            // No action needed
            is GuardianPhase.Complete -> GuardianUIState.COMPLETE

            // recovery, participantId is mandatory
            is GuardianPhase.RecoveryRequested -> if (participantId.isEmpty()) GuardianUIState.COMPLETE else GuardianUIState.ACCESS_REQUESTED
            is GuardianPhase.RecoveryVerification -> if (participantId.isEmpty()) GuardianUIState.COMPLETE else GuardianUIState.ACCESS_WAITING_FOR_TOTP_FROM_OWNER
            is GuardianPhase.RecoveryConfirmation -> if (participantId.isEmpty()) GuardianUIState.COMPLETE else GuardianUIState.ACCESS_VERIFYING_TOTP_FROM_OWNER
        }


        //Logging data for testing
        val stateParticipantId = guardianState.participantId.value
        projectLog(tag = routingLogTag, message = "Approver invite code: $inviteCode")
        projectLog(
            tag = routingLogTag,
            message = "Approver local participantID: ${participantId.ifEmpty { "no local value" }} (Retrieved from local storage)"
        )
        projectLog(
            tag = routingLogTag,
            message = "Approver remote participantID: ${stateParticipantId.ifEmpty { "no remote value" }} (From guardianState/remote)"
        )
        projectLog(tag = routingLogTag, message = "determined UI State: $existingApproverUIState")

        //Trigger navigation to move the approver forward
        triggerNavigation(existingApproverUIState)
    }

    private fun triggerNavigation(guardianUIState: GuardianUIState) {
        projectLog(tag = routingLogTag, message = "Triggering navigation to GuardianHomeVM")
        state = state.copy(navToGuardianHome = Resource.Success(guardianUIState))
    }

    fun resetNavigationTrigger() {
        projectLog(tag = routingLogTag, message = "Resetting navigation to GuardianHomeVM")
        state = state.copy(navToGuardianHome = Resource.Uninitialized)
    }
}