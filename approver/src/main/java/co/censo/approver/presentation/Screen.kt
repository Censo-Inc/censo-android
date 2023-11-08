package co.censo.approver.presentation

import co.censo.shared.DeepLinkURI

sealed class Screen(val route: String) {
    object ApproverEntranceRoute : Screen("approver_entrance_screen")
    object ApproverOnboardingScreen : Screen("approver_onboarding_screen")
    object ApproverAccessScreen : Screen("approver_access_screen")

    companion object {
        const val APPROVER_DEEPLINK_INVITATION = "approverDeepLinkInvitation"
        const val DL_INVITATION_ID_KEY = "invitation_id_key"

        const val APPROVER_DEEPLINK_ACCESS = "approverDeepLinkAccess"
        const val DL_PARTICIPANT_ID_KEY = "participant_id_key"

    }
}
