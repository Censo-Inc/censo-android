package co.censo.approver.presentation

import co.censo.shared.SharedScreen

fun Screen.buildScreenDeepLinkUri() = "${SharedScreen.APPROVER_INVITE_URI}${this.route}"

sealed class Screen(val route: String) {
    object ApproverEntranceRoute : Screen("approver_entrance_screen")
    object ApproverOnboardingScreen : Screen("approver_onboarding_screen")
    object ApproverAccessScreen : Screen("approver_access_screen")

    companion object {
        const val GUARDIAN_DEEPLINK_ACCEPTANCE = "guardianDeepLinkAcceptance"
        const val DL_INVITATION_ID_KEY = "invitation_id_key"

        const val GUARDIAN_DEEPLINK_RECOVERY = "guardianDeepLinkRecovery"
        const val DL_PARTICIPANT_ID_KEY = "participant_id_key"

    }
}
