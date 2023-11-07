package co.censo.approver.presentation.routing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.censo.shared.data.Resource
import co.censo.shared.data.repository.GuardianRepository
import co.censo.shared.data.repository.OwnerRepository
import co.censo.shared.util.projectLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 *
 * If we came from a link then navigate to the appropriate place
 * If not then have them paste a link and use that to navigate
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
        determineRoute()
    }

    fun onStop() {
        state = ApproverRoutingState()
    }

    private fun determineRoute() {
        viewModelScope.launch {
            val participantId = guardianRepository.retrieveParticipantId()
            val invitationId = guardianRepository.retrieveInvitationId()

            when {
                participantId.isEmpty() && invitationId.isEmpty() -> {
                    val userResponse = ownerRepository.retrieveUser()
                    if (userResponse is Resource.Success) {
                        state = state.copy(hasApprovers = userResponse.data!!.guardianStates.count { it.invitationId != null } > 0)
                    }
                    state = state.copy(showPasteLink = true)
                }
                participantId.isNotEmpty() ->
                    triggerNavigation(RoutingDestination.ACCESS)
                invitationId.isNotEmpty() ->
                    triggerNavigation(RoutingDestination.ONBOARDING)

            }
        }
    }

    fun userPastedLink(clipboardContent: String?) {

        if (clipboardContent == null) {
            state = state.copy(linkError = true)
            return
        }
        viewModelScope.launch {
            try {
                val censoLink = parseLink(clipboardContent)
                when(censoLink.host) {
                    "invite" -> {
                        guardianRepository.clearParticipantId()
                        guardianRepository.saveInvitationId(censoLink.identifier)
                        triggerNavigation(RoutingDestination.ONBOARDING)
                    }

                    "access" -> {
                        guardianRepository.clearInvitationId()
                        guardianRepository.saveParticipantId(censoLink.identifier)
                        triggerNavigation(RoutingDestination.ACCESS)
                    }

                    else -> state = state.copy(linkError = true)
                }
            } catch (e: Exception) {
                state = state.copy(linkError = true)
            }
        }
    }

    data class CensoLink(
        val host: String,
        val identifier: String
    )
    private fun parseLink(link: String): CensoLink {
        val parts = link.split("//")
        if (parts.size != 2 || !parts[0].startsWith("censo")) {
            throw Exception("invalid link")
        }
        val routeAndIdentifier = parts[1].split("/")
        if (routeAndIdentifier.size != 2 && !setOf("access", "invite").contains(routeAndIdentifier[0])) {
            throw Exception("invalid link")
        }
        return CensoLink(routeAndIdentifier[0], routeAndIdentifier[1])
    }

    fun clearError() {
        state = state.copy(linkError = false)
    }

    private fun triggerNavigation(routingDestination: RoutingDestination) {
        state = when (routingDestination) {
            RoutingDestination.ACCESS ->
                state.copy(navToApproverAccess = Resource.Success(Unit))

            RoutingDestination.ONBOARDING ->
                state.copy(navToApproverOnboarding = Resource.Success(Unit))
        }
    }

    fun resetApproverAccessNavigationTrigger() {
        state = state.copy(navToApproverAccess = Resource.Uninitialized)
    }

    fun resetApproverOnboardingNavigationTrigger() {
        state = state.copy(navToApproverOnboarding = Resource.Uninitialized)
    }
}