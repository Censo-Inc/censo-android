package co.censo.approver.presentation

import co.censo.shared.util.NavigationData
import ParticipantId

sealed class Screen(val route: String) {
    object ApproverEntranceRoute : Screen("approver_entrance_screen")
    object ApproverOnboardingScreen : Screen("approver_onboarding_screen")
    object ApproverAccessScreen : Screen("approver_access_screen")
    object ApproverSettingsScreen : Screen("approver_settings_screen")
    object ApproverOwnersListScreen : Screen("approver_owners_list_screen")
    object ApproverResetLinksScreen : Screen("approver_reset_links_screen")
    object ApproverLabelOwnerScreen : Screen("approver_owners_list_screen/{participantId}") {
        fun navTo(participantId: ParticipantId): NavigationData =
            NavigationData(
                route = route.replace("{participantId}", participantId.value),
                popSelfFromBackStack = false,
                popUpToTop = false
            )
    }

    companion object {
        const val APPROVER_DEEPLINK_INVITATION = "approverDeepLinkInvitation"
        const val DL_INVITATION_ID_KEY = "invitation_id_key"
        const val EMBEDDED_LINK_ID_KEY = "embedded_link_id_key"

        const val APPROVER_DEEPLINK_ACCESS = "approverDeepLinkAccess"
        const val APPROVER_V2_DEEPLINK_ACCESS = "approverV2DeepLinkAccess"
        const val DL_PARTICIPANT_ID_KEY = "participant_id_key"
        const val DL_APPROVAL_ID_KEY = "approval_id_key"

        const val APPROVER_UNIVERSAL_DEEPLINK = "approverUniversalDeepLink"
    }

    fun navTo() : NavigationData {
        return NavigationData(
            route = this.route,
            popSelfFromBackStack = false,
            popUpToTop = false
        )
    }


    fun navToAndPopCurrentDestination() : NavigationData {
        return NavigationData(
            route = this.route,
            popSelfFromBackStack = true,
            popUpToTop = false
        )
    }
}
